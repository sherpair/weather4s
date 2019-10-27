package io.sherpair.w4s.engine

import io.sherpair.w4s.domain.Locality
import org.apache.lucene.search.suggest.{Lookup => Suggester}

package object memory {

  type OSuggester = Option[Suggester]
  type Suggesters = List[OSuggester]

  case class DataSuggesters(
    suggesters: Suggesters,
    localities: Map[String, Locality]
  )
}
