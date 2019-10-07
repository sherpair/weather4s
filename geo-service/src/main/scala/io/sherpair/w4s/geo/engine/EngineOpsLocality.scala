package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{Country, Suggestions}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.LocalityIndex

private[engine] class EngineOpsLocality[F[_]: Sync](implicit E: Engine[F]) {

  private[engine] val localityIndex: F[LocalityIndex[F]] = E.localityIndex

  def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    localityIndex.flatMap(_.suggest(country, localityTerm, parameters))

  def suggestByAsciiOnly(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    localityIndex.flatMap(_.suggestByAsciiOnly(country, localityTerm, parameters))
}

object EngineOpsLocality {
  def apply[F[_]: Sync](implicit E: Engine[F]): EngineOpsLocality[F] = new EngineOpsLocality[F]
}
