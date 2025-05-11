package com.counterfly.logging

import com.typesafe.config.Config

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
