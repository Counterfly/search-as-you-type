package com.counterfly.logging.filter

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.util.Properties
import org.apache.kafka.streams.StreamsConfig

case class ServerConfig(
  topicLogReceived: String,
  topicLogFiltered: String,
  kafkaConsumer: KafkaStreamConfig,
)

object ServerConfig {

  def apply(config: Config, path: String): ServerConfig = {
    val serverConfig: Config = config.getConfig(path)

    new ServerConfig(
      topicLogReceived = serverConfig.getString("topic-log-received"),
      topicLogFiltered = serverConfig.getString("topic-log-filtered"),
      kafkaConsumer = KafkaStreamConfig(serverConfig.getConfig("kafka-consumer")),
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

case class KafkaStreamConfig(
  config: Config, // Since kafka has it's own config system, we just leverage that.
) {
  import KafkaConfigKeys._

  def build(): StreamsConfig = {
    val props = new Properties()

    // Required configurations
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString(KafkaBootstrapServersKey))
    // props.put(StreamsConfig.GROUP_ID_CONFIG, config.getString(KafkaGroupIdKey))
    // props.put(StreamsConfig.AUTO_OFFSET_RESET_CONFIG, config.getString(KafkaAutoOffsetResetKey))

    // Give the Streams application a unique name
    // The name must be unique in the Kafka cluster against which the application is run.
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "log-filter-docker");
    props.put(StreamsConfig.CLIENT_ID_CONFIG, "client-id-1");

    // Records should be flushed every 10 seconds. This is less than the default
    // in order to keep this example interactive.
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000)

    new StreamsConfig(props);
  }
}
object KafkaStreamConfig {
  def apply(config: Config) = new KafkaStreamConfig(config)
}

object KafkaConfigKeys {
  val KafkaBootstrapServersKey = "kafka.bootstrap.servers"
  val KafkaGroupIdKey = "kafka.group.id"
  val KafkaAutoOffsetResetKey = "kafka.auto.offset.reset"
  val KafkaTopicKey = "kafka.topic"
  val PollDurationMsKey = "kafka.poll.duration.ms"
}

// case class KafkaProducerConfig(
//   bootstrapServers: String, // in form "host:port,host:port"
//   acks: String,
//   maxInFlightRequests: Int,
// )

// object KafkaProducerConfig {

//   def apply(config: Config) = new KafkaProducerConfig(
//     bootstrapServers = config.getString("bootstrap-servers"),
//     acks = config.getString("acks"),
//     maxInFlightRequests = config.getInt("max-in-flight-requests"),
//   )
// }
