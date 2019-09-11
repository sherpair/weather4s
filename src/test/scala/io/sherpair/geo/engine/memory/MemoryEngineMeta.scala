package io.sherpair.geo.engine.memory

import cats.ApplicativeError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._
import io.sherpair.geo.domain.Meta
import io.sherpair.geo.engine.EngineMeta

class MemoryEngineMeta[F[_]](ref: Ref[F, Option[Meta]])(implicit ae: ApplicativeError[F, Throwable]) extends EngineMeta[F] {

  override def getById: F[Option[Meta]] = ref.get

  override def upsert(meta: Meta): F[String] = ref.set(Some(meta)) *> "OK".pure

  private[memory] def refCount: F[Long] = 1L.pure[F]
}

object MemoryEngineMeta {
  def apply[F[_]: Sync]: F[MemoryEngineMeta[F]] =
    Ref.of[F, Option[Meta]](None).map(new MemoryEngineMeta[F](_))
}