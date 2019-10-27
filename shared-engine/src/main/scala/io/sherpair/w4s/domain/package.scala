package io.sherpair.w4s

import io.sherpair.w4s.domain.{Country, Locality, Suggestion}

package object types {

  type Countries = List[Country]
  type Localities = List[Locality]
  type Suggestions = List[Suggestion]
}
