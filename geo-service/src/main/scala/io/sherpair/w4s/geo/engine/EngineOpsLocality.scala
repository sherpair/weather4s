package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.Country
import io.sherpair.w4s.engine.LocalityIndex
import io.sherpair.w4s.types.Suggestions

private[engine] class EngineOpsLocality[F[_]: Sync](localityIndex: LocalityIndex[F]) {

  def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    localityIndex.suggest(country, localityTerm, parameters)

  def suggestByAsciiOnly(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    localityIndex.suggestByAsciiOnly(country, localityTerm, parameters)
}

object EngineOpsLocality {

  def apply[F[_]: Sync](localityIndex: LocalityIndex[F]): F[EngineOpsLocality[F]] =
    Sync[F].delay(new EngineOpsLocality[F](localityIndex))
}
