package io.sherpair.w4s.engine.elastic

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

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
import io.circe.{Decoder, Encoder}
import io.circe.jawn.decode
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{jsonPrinter, W4sError}
import io.sherpair.w4s.engine.{Engine, EngineIndex, LocalityIndex}

class ElasticEngine[F[_]: Async: Timer] private[elastic] (
    elasticClient: ElasticClient)(implicit C: Configuration) extends Engine[F] {

  override def close: F[Unit] = Async[F].delay(elasticClient.close)

  override def createIndex(name: String, jsonMapping: Option[String]): F[Unit] =
    elasticClient.execute(ElasticDsl.createIndex(name).copy(rawSource = jsonMapping)).lift.void

  def engineIndex[T: ClassTag: Decoder: Encoder](indexName: String, f: T => String): EngineIndex[F, T] = {

    implicit val aggReader: AggReader[T] = (json: String) => decode[T](json).fold(Failure(_), Success(_))
    implicit val hitReader: HitReader[T] = (hit: Hit) => decode[T](hit.sourceAsString).fold(Failure(_), Success(_))
    implicit val indexable: Indexable[T] = (t: T) => jsonPrinter(Encoder[T].apply(t))

    new ElasticEngineIndex[F, T](elasticClient, indexName, f, C.engine)
  }

  override def execUnderGlobalLock[T](f: => F[T]): F[T] =
    acquireLock(1.max(C.lockAttempts)).ifM(
      Resource.make(Async[F].unit)(_ => releaseLock).use(_ => f),
      if (C.lockGoAhead) f
      else Async[F].raiseError[T](W4sError("Can't acquire a global lock for the ES Engine"))
    )

  override def healthCheck: F[(Int, String)] =
    attemptHealthCheck(C.healthAttempts, C.healthInterval)

  override def indexExists(name: String): F[Boolean] =
    elasticClient.execute(ElasticApi.indexExists(name)).lift.map(_.result.exists)

  override def localityIndex: LocalityIndex[F] = new ElasticLocalityIndex[F](elasticClient)

  override def refreshIndex(name: String): F[Boolean] =
    elasticClient.execute(ElasticApi.refreshIndex(name)).lift.map(_.isSuccess)

  private def acquireLock(lockAttempts: Int): F[Boolean] =
    Async[F].tailRecM[Int, Boolean](lockAttempts) { lockAttempt =>
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
          W4sError(s"Engine Health check failed after ${C.healthAttempts} attempts", Some(error))
        )

      case Right(response) => Async[F].delay((C.healthAttempts - attempts + 1, response.result.status))
    }

  private def isGlobalLockAcquired(acquired: Boolean, lockAttempt: Int): Either[Int, Boolean] =
    if (acquired) true.asRight[Int]
    else if (lockAttempt == 0) false.asRight[Int]
    else {
      Timer[F].sleep(C.lockInterval)
      (lockAttempt - 1).asLeft[Boolean]
    }

  private def releaseLock: F[Unit] = elasticClient.execute(releaseGlobalLock()).map(_ => ()).lift
}

object ElasticEngine {
  def apply[F[_]: Async: Timer](implicit C: Configuration): Engine[F] = {
    val cluster: String = s"cluster.name=${C.clusterName}"

    val elasticProperties: ElasticProperties =
      ElasticProperties(s"http://${C.engine.host.address}:${C.engine.host.port}?${cluster}")

    val elasticClient: ElasticClient = ElasticClient(JavaClient(elasticProperties))

    new ElasticEngine[F](elasticClient)
  }
}
