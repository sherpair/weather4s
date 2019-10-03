package io.sherpair.w4s.loader.app

import cats.effect.{Concurrent, ContextShift, Fiber, Resource, Sync}
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.domain.{startOfTheDay, unit, Country, Logger}
import io.sherpair.w4s.domain.Country.countryUnderLoadOrUpdate
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.domain.LoaderContext
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.client.Client

class Loader[F[_]: ContextShift: Engine: Sync](
    client: Client[F], queue: NoneTerminatedQueue[F, Country])(
    implicit C: LoaderConfig, engineOps: EngineOps[F], L: Logger[F]
) {

  val start: F[Unit] = queue.dequeue.evalMap[F, Unit](gotCountry).compile.drain

  private def gotCountry(country: Country): F[Unit] =
    for {
      maybeCountry <- engineOps.engineOpsCountries.find(country)
      _ <- isValidUpsertCountryRequest(country, maybeCountry).ifM(
        LoaderRun[F](client, country),
        L.debug(s"${country} unknown or under load/update or recently added/updated"))
    }
    yield unit

  private def isValidUpsertCountryRequest(country: Country, maybeCountry: Option[Country]): F[Boolean] =
    maybeCountry.fold(false) { mC =>
      mC.updated != countryUnderLoadOrUpdate &&
      mC.updated < startOfTheDay &&
      mC.updated <= country.updated  // Unnecessary, still...
    }.pure[F]
}

object Loader {

  def apply[F[_]: Concurrent: Engine: Logger](
      client: Client[F], queue: NoneTerminatedQueue[F, Country])(
      implicit C: LoaderConfig, CL: LoaderContext[F], engineOps: EngineOps[F]
  ): Resource[F, Fiber[F, Unit]] = {

    implicit val cs: ContextShift[F] = CL.cs
    Resource.liftF(cs.evalOn(CL.ec)(new Loader[F](client, queue).start.start))
  }
}
