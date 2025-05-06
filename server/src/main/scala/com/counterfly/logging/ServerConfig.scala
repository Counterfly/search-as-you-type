package com.counterfly.logging

import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.util.Await
import com.twitter.util.Future
import com.twitter.util.Managed
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ServerConfig(
  topicLogReceived: String,
  kafkaProducerConfig: KafkaProducerConfig,
)

object ServerConfig {

  def apply(config: Config, path: String): ServerConfig = {
    val serverConfig: Config = config.getConfig(path)

    ServerConfig(
      topicLogReceived = serverConfig.getString("topic-log-received"),
      kafkaProducerConfig = KafkaProducerConfig(
        serverConfig.getConfig("kafka-producer"),
      ),
    )
  }
}

case class KafkaProducerConfig(
  bootstrapServers: String, // in form "host:port,host:port"
  acks: String,
  maxInFlightRequests: Int,
)

object KafkaProducerConfig {

  def apply(config: Config) = new KafkaProducerConfig(
    bootstrapServers = config.getString("kafka.bootstrap-servers"),
    acks = config.getString("kafka.acks"),
    maxInFlightRequests = config.getInt("kafka.max-in-flight-requests"),
  )
}
