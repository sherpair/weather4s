package io.sherpair.w4s.loader.domain

import scala.concurrent.ExecutionContext

import cats.effect.ContextShift

case class LoaderContext[F[_]](cs: ContextShift[F], ec: ExecutionContext)
