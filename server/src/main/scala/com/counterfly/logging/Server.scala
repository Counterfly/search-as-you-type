package com.counterfly.logging

import com.counterfly.common.FlagConfig
import com.counterfly.common.MyServer
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.util.Await
import com.twitter.util.Closable
import com.twitter.util.Future
import com.twitter.util.Managed
import com.twitter.util.Time
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import ingest.IngestController
import ingest.IngestService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.producer.KafkaProducer

object Server extends MyServer with FlagConfig {
  override val name = "search-as-you-type"
  override val disableAdminHttpServer: Boolean = true

  // Override the default Finatra HTTP port (default is :8888)
  // Override this by specifing `-http.port=1234` in the command line
  // override def defaultHttpPort: String = ":7000"

  private[this] lazy val serverConfig: ServerConfig =
    ServerConfig.parseConfig(config.getConfig(name))

  // todo: Create IngestService as a singleton to manage its lifecycle
  private[this] lazy val logController = new IngestController(new IngestService(
    producer = IngestService.createProducer(
      serverConfig.kafkaProducerConfig,
    ),
    topic = serverConfig.topicLogReceived,
  ))

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .add(logController)
  }
}
