package com.counterfly.logging.store

import com.counterfly.logging.filter.TerminalWordDeserializer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig

case class ServerConfig(
  topicLogFiltered: String,
  kafkaConsumer: KafkaConsumerConfig,
)

object ServerConfig {

  def apply(config: Config, path: String): ServerConfig = {
    val serverConfig: Config = config.getConfig(path)

    new ServerConfig(
      topicLogFiltered = serverConfig.getString("topic-log-filtered"),
      kafkaConsumer = KafkaConsumerConfig(serverConfig.getConfig("kafka-consumer")),
    )
  }

  def fromArgs(args: Array[String], path: String): ServerConfig = {
    // Process command line arguments
    val configOpt = parseArgs(args)

    // Load configuration based on whether a custom path was provided
    configOpt match {
      case None => apply(ConfigFactory.load(), path)
      case Some(value) => apply(ConfigFactory.load(value), path)
    }
  }

  /**
   * Parse command line arguments to extract config file path
   * Supports formats:
   *  -config=/path/to/config.conf
   *  -config /path/to/config.conf
   */
  private def parseArgs(args: Array[String]): Option[String] = {
    if (args.isEmpty) {
      return None
    }

    // Try to find -config argument
    for (i <- 0 until args.length) {
      val arg = args(i)

      // Handle -config=path format
      if (arg.startsWith("-config=")) {
        return Some(arg.substring("-config=".length))
      }

      // Handle -config path format (requires next arg)
      if (arg == "-config" && i + 1 < args.length) {
        return Some(args(i + 1))
      }
    }

    None
  }
}

case class KafkaConsumerConfig(
  config: Config, // Since kafka has it's own config system, we just leverage that.
) {

  def build(): Properties = {
    val props = new Properties()

    // Required configurations
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
    props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getString(ConsumerConfig.GROUP_ID_CONFIG))
    // props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getString(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG))

    // Give the Streams application a unique name
    // The name must be unique in the Kafka cluster against which the application is run.
    // props.put(ConsumerConfig.APPLICATION_ID_CONFIG, "log-filter-docker");
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "client-id-1");

    // Records should be flushed every 10 seconds. This is less than the default
    // in order to keep this example interactive.
    // props.put(ConsumerConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000)

    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      classOf[org.apache.kafka.common.serialization.StringDeserializer],
    );
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      classOf[TerminalWordDeserializer],
    );

    props
  }
}
object KafkaConsumerConfig {
  def apply(config: Config) = new KafkaConsumerConfig(config)
}
