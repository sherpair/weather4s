package io.sherpair.w4s

import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

package object app {

  val MT: `Content-Type` = `Content-Type`(MediaType.application.json)
}
