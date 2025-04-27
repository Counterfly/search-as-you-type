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

  def apply(configName: String = "development"): ServerConfig = {
    val config: Config = ConfigFactory.load(s"$configName.conf")

    ServerConfig(
      topicLogReceived = config.getString("topic-log-received"),
      kafkaProducerConfig = KafkaProducerConfig(
        config.getConfig("kafka-producer"),
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
    bootstrapServers = config.getString("bootstrap-servers"),
    acks = config.getString("acks"),
    maxInFlightRequests = config.getInt("max-in-flight-requests"),
  )
}
