package io.sherpair.w4s.auth

import doobie.free.connection.ConnectionIO
import fs2.Stream
import io.sherpair.w4s.auth.domain.Record

package object repository {

  type Result[F[_], V] = F[ConnectionIO[V]]

  type ResultList[F[_], K, R <: Record[K]] = F[ConnectionIO[List[R]]]

  type ResultOption[F[_], K, R <: Record[K]] = F[ConnectionIO[Option[R]]]

  type ResultRecord[F[_], K, R <: Record[K]] = F[ConnectionIO[R]]

  type ResultStream[K, R <: Record[K]] = Stream[ConnectionIO, R]
}
