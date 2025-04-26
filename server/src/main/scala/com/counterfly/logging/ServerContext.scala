package com.counterfly.logging

import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Managed
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ServerContext()
