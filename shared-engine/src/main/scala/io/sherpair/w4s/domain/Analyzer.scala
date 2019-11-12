package io.sherpair.w4s.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}

/* Languages supported by the ElasticSearch's Analyzers */
sealed trait Analyzer extends EnumEntry

// scalastyle:off number.of.methods
object Analyzer extends Enum[Analyzer] with CirceEnum[Analyzer] {

  val values = findValues

  case object arabic extends Analyzer
  case object armenian extends Analyzer
  case object basque extends Analyzer
  case object bengali extends Analyzer
  case object brazilian extends Analyzer
  case object bulgarian extends Analyzer
  case object catalan extends Analyzer
  case object cjk extends Analyzer
  case object czech extends Analyzer
  case object danish extends Analyzer
  case object dutch extends Analyzer
  case object english extends Analyzer
  case object finnish extends Analyzer
  case object french extends Analyzer
  case object galician extends Analyzer
  case object german extends Analyzer
  case object greek extends Analyzer
  case object hindi extends Analyzer
  case object hungarian extends Analyzer
  case object indonesian extends Analyzer
  case object irish extends Analyzer
  case object italian extends Analyzer
  case object latvian extends Analyzer
  case object lithuanian extends Analyzer
  case object norwegian extends Analyzer
  case object persian extends Analyzer
  case object portuguese extends Analyzer
  case object romanian extends Analyzer
  case object russian extends Analyzer
  case object sorani extends Analyzer
  case object spanish extends Analyzer
  case object swedish extends Analyzer
  case object turkish extends Analyzer
  case object thai extends Analyzer

  case object simple extends Analyzer
  case object standard extends Analyzer
  case object stop extends Analyzer
}
// scalastyle:on
