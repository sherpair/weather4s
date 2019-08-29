package io.sherpair.geo.engine

import cats.Monad
import cats.effect.{Async, IO, Resource, Timer}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.sksamuel.elastic4s.{ElasticApi, ElasticClient, ElasticDsl, ElasticProperties}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.get.{GetRequest, GetResponse}
import com.sksamuel.elastic4s.requests.indexes.{IndexRequest, IndexResponse}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration._
import io.sherpair.geo.domain.GeoError

class ElasticEngine[F[_]: Async: Timer] private (elasticClient: ElasticClient)(implicit config: Configuration) extends Engine[F] {

  def add(indexRequest: IndexRequest): F[IndexResponse] =
    for {
      response <- elasticClient.execute(indexRequest).lift
    } yield response.result

  def addAll(indexRequests: Seq[IndexRequest]): F[Option[String]] =
    for {
      response <- elasticClient.execute(bulk(indexRequests)).lift
    } yield if (response.result.hasFailures) response.body else None

  def close: F[Unit] = Async[F].delay(elasticClient.close)

  def count(indexName: String): F[Long] =
    for {
      response <- elasticClient.execute(ElasticApi.count(indexName)).lift
    } yield response.result.count

  def createIndex(name: String, jsonMapping: String): F[Unit] =
    for {
      _ <- elasticClient.execute(ElasticDsl.createIndex(name).source(jsonMapping)).lift
    } yield ()

  def execUnderGlobalLock[T](f: => F[T]): F[T] =
    acquireLock(math.max(1, lockAttempts(config))).ifM(
      Resource.make(Async[F].unit)(_ => releaseLock).use(_ => f),
      if (lockGoAhead(config)) f
      else Async[F].raiseError(GeoError("Can't acquire a global lock for the ES Engine"))
    )

  def getById(indexName: String, id: String): F[GetResponse] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield response.result

  def healthCheck: F[String] =
    for {
      response <- elasticClient.execute(clusterHealth()).lift
    } yield response.result.status

  def indexExists(name: String): F[Boolean] =
    for {
      response <- elasticClient.execute(ElasticApi.indexExists(name)).lift
    } yield response.result.exists

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  def queryAll(indexName: String, sortBy: Option[Seq[String]], windowSize: Int): F[SearchResponse] = {
    val _windowSize = Math.max(1, Math.min(MaxWindowSize, windowSize))
    val sorts: Seq[FieldSort] = sortBy.map(_.map(fieldSort(_))).getOrElse(Seq.empty)

    for {
      response <- elasticClient.execute(search(indexName).query(matchAllQuery()).sortBy(sorts) size (_windowSize)).lift
    } yield response.result
  }

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

  private implicit class IOLifter[A](val io: IO[A]) {
    def lift: F[A] = Async[F].liftIO(io)
  }
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
