package io.sherpair.w4s.domain

import java.security.PublicKey

import pdi.jwt.algorithms.JwtRSAAlgorithm

case class AuthData(
  jwtAlgorithm: JwtRSAAlgorithm,
  jwtAlgorithms: List[JwtRSAAlgorithm],
  publicKey: PublicKey
)

object AuthData {
  def apply(jwtAlgorithm: JwtRSAAlgorithm, publicKey: PublicKey): AuthData =
    new AuthData(jwtAlgorithm, List(jwtAlgorithm), publicKey)
}
