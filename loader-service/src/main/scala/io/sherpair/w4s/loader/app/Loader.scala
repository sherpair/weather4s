package io.sherpair.w4s.loader.app

import cats.effect.{Blocker, Concurrent, ContextShift, Fiber, Resource, Sync}
import cats.effect.syntax.concurrent._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.domain.{startOfTheDay, unit, Country, Logger}
import io.sherpair.w4s.domain.Country.countryUnderLoadOrUpdate
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.client.Client

class Loader[F[_]: ContextShift: Sync](
    blocker: Blocker, client: Client[F], queue: NoneTerminatedQueue[F, Country])(
    implicit C: LoaderConfig, engineOps: EngineOps[F], L: Logger[F]
) {

  val start: F[Unit] = queue.dequeue.evalMap[F, Unit](gotCountry).compile.drain

  private def gotCountry(country: Country): F[Unit] =
    for {
      maybeCountry <- engineOps.findCountry(country)
      _ <- isValidUpsertCountryRequest(country, maybeCountry).ifM(
        LoaderRun[F](blocker, client, country),
        L.debug(s"${country} unknown or under load/update or recently added/updated"))
    }
    yield unit

  private def isValidUpsertCountryRequest(country: Country, maybeCountry: Option[Country]): F[Boolean] =
    Sync[F].delay(maybeCountry.fold(false) { mC =>
      mC.updated != countryUnderLoadOrUpdate &&
      mC.updated < startOfTheDay &&
      mC.updated <= country.updated  // Unnecessary, still...
    })
}

object Loader {

  def apply[F[_]: Concurrent: ContextShift](
      client: Client[F], queue: NoneTerminatedQueue[F, Country])(
      implicit B: Blocker, C: LoaderConfig, engineOps: EngineOps[F], L: Logger[F]
  ): Resource[F, Fiber[F, Unit]] =
    Resource.liftF(B.blockOn(new Loader[F](B, client, queue).start.start))
}
