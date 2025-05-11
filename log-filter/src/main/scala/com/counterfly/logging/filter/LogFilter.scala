package com.counterfly.logging.filter

import org.slf4j.LoggerFactory
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * A Kafka consumer application that filters and processes log messages.
 */
object LogFilter {
  val name = "log-filter"
  private val logger = LoggerFactory.getLogger(getClass)

  // Message buffer for windowing
  // private val messageBuffer = ListBuffer.empty[LogMessage]

  // case class LogMessageWithRecord(
  //   record: ConsumerRecord[String, LogMessage],
  //   logMessage: LogMessage,
  // )

  def main(args: Array[String]): Unit = {
    Try {
      // Load configuration
      val config: ServerConfig = ServerConfig.fromArgs(args, name)

      logger.info("Starting LogFilter Kafka consumer with hopping windows")

      // Create stream
      // TODO: wrap in processable/consumer trait
      val inputStream = KafkaProcessor(
        streamProperties = config.kafkaConsumer.build(),
        inputTopic = config.topicLogReceived,
        outputTopic = config.topicLogFiltered,
      )

      // Set up shutdown hook for graceful termination
      Runtime.getRuntime.addShutdownHook(new Thread(() => {
        logger.info("Shutting down Kafka consumer")
        // TODO: consider if need to process any remaining messages in the buffer
        // processWindowedMessages()
        inputStream.close()
      }))

      // start processing
      inputStream.process()
    } match {
      case Success(_) => ()
      case Failure(e) =>
        logger.error(s"Fatal error in LogFilter", e)
        System.exit(1)
    }
  }
}
