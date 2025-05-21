package com.counterfly.logging.store

import org.slf4j.LoggerFactory
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * A Kafka consumer application that filters and processes log messages.
 */
object LogStore {
  val name = "log-store"
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    Try {
      // Load configuration
      val config: ServerConfig = ServerConfig.fromArgs(args, name)

      logger.info("Starting LogStore Kafka consumer with hopping windows")

      // Create consumer
      val processor = KafkaLogConsumer(
        consumerProperties = config.kafkaConsumer.build(),
        inputTopic = config.topicLogFiltered,
      )

      // TODO: storeService: LogStoreService

      // Set up shutdown hook for graceful termination
      Runtime.getRuntime.addShutdownHook(new Thread(() => {
        logger.info("Shutting down Kafka consumer")
        // TODO: consider if need to process any remaining messages in the buffer
        processor.close()
      }))

      process(
        consumer = processor,
        // TODO: make this a config
        sleepDurationMs = 500,
      )
    } match {
      case Success(_) => ()
      case Failure(e) =>
        logger.error(s"Fatal error in LogStore", e)
        System.exit(1)
    }
  }

  private def process(
    consumer: LogConsumer,
    sleepDurationMs: Long,
  ): Unit = {
    // start processing
    while (true) {
      // Sleep after processing a batch of records
      // This reduces CPU usage when there are no messages
      try {
        // Sleep for 500ms before polling again
        Thread.sleep(sleepDurationMs)
      } catch {
        case _: InterruptedException =>
          Thread.currentThread().interrupt()
      }

      val result =
        (for {
           // TODO: will need to lift to EitherT
           records <- consumer.process()
           _ <- storeRecords(records) // TODO: Future
           // Commit offsets when data is successfully stored
           _ = consumer.commit()
         } yield ())

      // TODO: just for manual testing, remove this block
      result match {
        case Left(message) =>
          logger.info(s"Did not store any records: $message")
        case Right(_) => ()
      }
    }
  }

  /**
   * TODO EitherT w Future
   *
   * Wrapper around storing new records.
   * Catches all exceptions, logs them and returns safely.
   */
  private def storeRecords(
    @scala.annotation.unused
    records: List[Record],
  ): Either[String, Unit] = {
    Right(())
  }
}
