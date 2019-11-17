package io.sherpair.w4s.domain

import java.security.PublicKey

import pdi.jwt.algorithms.JwtRSAAlgorithm

case class DataForAuthorisation(
  jwtAlgorithm: JwtRSAAlgorithm,
  jwtAlgorithms: List[JwtRSAAlgorithm],
  publicKey: PublicKey
)

object DataForAuthorisation {
  def apply(jwtAlgorithm: JwtRSAAlgorithm, publicKey: PublicKey): DataForAuthorisation =
    new DataForAuthorisation(jwtAlgorithm, List(jwtAlgorithm), publicKey)
}
