package io.sherpair.w4s.http

import javax.net.ssl.SSLContext

import io.sherpair.w4s.config.Host

case class SSLData(host: Host, context: SSLContext)
