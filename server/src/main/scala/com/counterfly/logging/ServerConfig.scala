package com.counterfly.logging

import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Managed
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ServerConfig() {
  def build(): ServerContext = {
    new ServerContext()
  }
}

object ServerConfig {

  def apply(configName: String = "development"): ServerConfig = {
    val config: Config = ConfigFactory.load(s"$configName.conf")
    parseConfig(config)
  }

  def parseConfig(config: Config): ServerConfig = {
    ServerConfig()
  }
}
