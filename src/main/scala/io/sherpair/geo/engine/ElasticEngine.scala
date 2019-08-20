package io.sherpair.geo.engine

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import cats.effect.{Async, IO, Resource, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl, ElasticProperties, Response}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import io.sherpair.geo.algebra.Engine
import io.sherpair.geo.config.Configuration

class ElasticEngine[F[_]: Async](implicit config: Configuration) extends Engine[F] {

  private lazy val cluster: String = s"cluster.name=${config.elasticSearch.cluster.name}"

  private lazy val elasticProperties: ElasticProperties =
    ElasticProperties(s"http://${config.elasticSearch.host.address}:${config.elasticSearch.host.port}?${cluster}")

  private lazy val elasticClient: ElasticClient = ElasticClient(JavaClient(elasticProperties))

  def init: F[String] =
    for {
      response <- elasticClient.execute(clusterHealth()).lift
    } yield response.result.status

  def close: F[Unit] = Async[F].delay(elasticClient.close)

  def createIndexIfNotExists(name: String, mapping: MappingDefinition = emptyMapping): F[Boolean] = {
    implicit val timer = IO.timer(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))
    createIndexIfNotExistsLoop(
      name,
      mapping,
      math.max(1, config.elasticSearch.globalLock.attempts),
      config.elasticSearch.globalLock.interval
    ).lift
  }

  private def runCreateIndexIfNotExists(name: String, mapping: MappingDefinition): IO[Boolean] =
    for {
      response <- elasticClient.execute(indexExists(name))
      created <- createIndexIfNotExists(name, mapping, response)
    } yield created

  private def createIndexIfNotExistsLoop(name: String, mapping: MappingDefinition, lockAttempts: Int, lockInterval: FiniteDuration)(
      implicit timer: Timer[IO]
  ): IO[Boolean] = {
    val lock = elasticClient
      .execute(acquireGlobalLock())
      .handleErrorWith { error =>
        if (lockAttempts > 0) IO.sleep(lockInterval) *> createIndexIfNotExistsLoop(name, mapping, lockAttempts - 1, lockInterval)
        else IO.raiseError(error)
      }

    Resource.make(lock)(_ => releaseLock).use(_ => runCreateIndexIfNotExists(name, mapping))
  }

  private def createIndexIfNotExists(name: String, mapping: MappingDefinition, response: Response[IndexExistsResponse]): IO[Boolean] =
    if (response.result.exists) IO.pure(false)
    else elasticClient.execute(ElasticDsl.createIndex(name).mapping(mapping)) *> IO.pure(true)

  private def releaseLock: IO[Unit] = elasticClient.execute(releaseGlobalLock()).map(_ => ())

  implicit class IOLifter[A](val io: IO[A]) {
    def lift: F[A] = Async[F].liftIO(io)
  }
}
