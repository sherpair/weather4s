package io.sherpair.w4s.engine

import cats.effect.{Async, IO}

package object elastic {

  private[elastic] implicit class IOLifter[F[_]: Async, T](val io: IO[T]) {
    def lift: F[T] = Async[F].liftIO(io)
  }
}
