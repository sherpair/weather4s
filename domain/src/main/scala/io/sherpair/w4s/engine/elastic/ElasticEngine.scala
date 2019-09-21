package io.sherpair.w4s.engine.elastic

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

import cats.Monad
import cats.effect.{Async, Resource, Timer}
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.sksamuel.elastic4s.{AggReader, ElasticApi, ElasticClient, ElasticDsl, ElasticProperties, Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.http.JavaClient
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.jawn.decode
import io.sherpair.w4s.config.{Engine => EngineConfig, Service}
import io.sherpair.w4s.config.Engine._
import io.sherpair.w4s.domain.{unit, W4sError}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

class ElasticEngine[F[_]: Async: Timer] private[elastic] (
    elasticClient: ElasticClient, EC: EngineConfig)(implicit S: Service) extends Engine[F] {

  override def close: F[Unit] = Async[F].delay(elasticClient.close)

  override def createIndex(name: String, jsonMapping: Option[String]): F[Unit] =
    for {
      _ <- elasticClient.execute(ElasticDsl.createIndex(name).copy(rawSource = jsonMapping)).lift
    } yield unit

  def engineIndex[T: ClassTag: Decoder: Encoder](indexName: String, f: T => String): EngineIndex[F, T] = {

    implicit val aggReader: AggReader[T] = (json: String) => decode[T](json).fold(Failure(_), Success(_))
    implicit val hitReader: HitReader[T] = (hit: Hit) => decode[T](hit.sourceAsString).fold(Failure(_), Success(_))
    implicit val indexable: Indexable[T] = (t: T) => printer(Encoder[T].apply(t))

    new ElasticEngineIndex[F, T](elasticClient, indexName, f, EC)
  }

  override def execUnderGlobalLock[T](f: => F[T]): F[T] =
    acquireLock(1.max(lockAttempts(EC))).ifM(
      Resource.make(Async[F].unit)(_ => releaseLock).use(_ => f),
      if (lockGoAhead(EC)) f
      else Async[F].raiseError[T](W4sError("Can't acquire a global lock for the ES Engine"))
    )

  override def healthCheck: F[(Int, String)] =
    attemptHealthCheck(healthAttempts(EC), healthInterval(EC))

  override def indexExists(name: String): F[Boolean] =
    for {
      response <- elasticClient.execute(ElasticApi.indexExists(name)).lift
    } yield response.result.exists

  private def acquireLock(lockAttempts: Int): F[Boolean] =
    Monad[F].tailRecM[Int, Boolean](lockAttempts) { lockAttempt =>
      for {
        response <- elasticClient.execute(acquireGlobalLock()).lift
        result <- isGlobalLockAcquired(response.result, lockAttempt).pure[F]
      } yield result
    }

  private def attemptHealthCheck(attempts: Int, interval: FiniteDuration): F[(Int, String)] =
    elasticClient.execute(clusterHealth()).attempt.lift.flatMap {
      case Left(error) =>
        if (attempts > 0) Timer[F].sleep(interval) *> attemptHealthCheck(attempts - 1, interval)
        else Async[F].raiseError[(Int, String)](
          W4sError(s"Engine Health check failed after ${healthAttempts(EC)} attempts", Some(error))
        )

      case Right(response) => Async[F].delay((healthAttempts(EC) - attempts + 1, response.result.status))
    }

  private def isGlobalLockAcquired(acquired: Boolean, lockAttempt: Int): Either[Int, Boolean] =
    if (acquired) true.asRight[Int]
    else if (lockAttempt == 0) false.asRight[Int]
    else {
      Timer[F].sleep(lockInterval(EC))
      (lockAttempt - 1).asLeft[Boolean]
    }

  private val printer: Json => String = Printer.noSpaces.copy(dropNullValues = true).print

  private def releaseLock: F[Unit] = elasticClient.execute(releaseGlobalLock()).map(_ => ()).lift
}

object ElasticEngine {
  def apply[F[_]: Async: Timer](EC: EngineConfig)(implicit S: Service): Engine[F] = {
    val cluster: String = s"cluster.name=${clusterName(EC)}"

    val elasticProperties: ElasticProperties =
      ElasticProperties(s"http://${EC.host.address}:${EC.host.port}?${cluster}")

    val elasticClient: ElasticClient = ElasticClient(JavaClient(elasticProperties))

    new ElasticEngine[F](elasticClient, EC)
  }
}
