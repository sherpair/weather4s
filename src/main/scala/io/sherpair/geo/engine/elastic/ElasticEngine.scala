package io.sherpair.geo.engine.elastic

import cats.Monad
import cats.effect.{Async, Resource, Timer}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.sksamuel.elastic4s.{ElasticApi, ElasticClient, ElasticDsl, ElasticProperties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.http.JavaClient
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration._
import io.sherpair.geo.domain.GeoError
import io.sherpair.geo.engine.{Engine, EngineCountry, EngineMeta}

class ElasticEngine[F[_]: Async: Timer] private[elastic] (elasticClient: ElasticClient)(implicit config: Configuration) extends Engine[F] {

  def close: F[Unit] = Async[F].delay(elasticClient.close)

  def count(indexName: String): F[Long] =
    for {
      response <- elasticClient.execute(ElasticApi.count(indexName)).lift
    } yield response.result.count

  def createIndex(name: String, jsonMapping: Option[String]): F[Unit] =
    for {
      _ <- elasticClient.execute(ElasticDsl.createIndex(name).copy(rawSource = jsonMapping)).lift
    } yield ()

  def engineCountry: EngineCountry[F] = new ElasticEngineCountry[F](elasticClient)

  def engineMeta: EngineMeta[F] = new ElasticEngineMeta[F](elasticClient)

  def execUnderGlobalLock[T](f: => F[T]): F[T] =
    acquireLock(math.max(1, lockAttempts(config))).ifM(
      Resource.make(Async[F].unit)(_ => releaseLock).use(_ => f),
      if (lockGoAhead(config)) f
      else Async[F].raiseError(GeoError("Can't acquire a global lock for the ES Engine"))
    )

  def healthCheck: F[String] =
    for {
      response <- elasticClient.execute(clusterHealth()).lift
    } yield response.result.status

  def indexExists(name: String): F[Boolean] =
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

  private def isGlobalLockAcquired(acquired: Boolean, lockAttempt: Int): Either[Int, Boolean] =
    if (acquired) Right[Int, Boolean](true)
    else if (lockAttempt == 0) Right[Int, Boolean](false)
    else {
      Timer[F].sleep(lockInterval(config))
      Left[Int, Boolean](lockAttempt - 1)
    }

  private def releaseLock: F[Unit] = elasticClient.execute(releaseGlobalLock()).map(_ => ()).lift
}

object ElasticEngine {
  def apply[F[_]: Async: Timer](implicit config: Configuration): ElasticEngine[F] = {
    val cluster: String = s"cluster.name=${clusterName(config)}"
    val host = config.elasticSearch.host

    val elasticProperties: ElasticProperties =
      ElasticProperties(s"http://${host.address}:${host.port}?${cluster}")

    val elasticClient: ElasticClient = ElasticClient(JavaClient(elasticProperties))

    new ElasticEngine[F](elasticClient)
  }
}
