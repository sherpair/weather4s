package io.sherpair.geo.domain

import io.chrisdavenport.log4cats.Logger

case class Meta(lastEngineUpdate: Long = epochAsLong) extends AnyVal {

  def logLastEngineUpdate[F[_]: Logger]: F[Unit] =
    Logger[F].info(s"Last Engine update at(${toIsoDate(lastEngineUpdate)})")
}
