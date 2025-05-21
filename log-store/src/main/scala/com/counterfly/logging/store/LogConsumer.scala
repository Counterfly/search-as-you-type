package com.counterfly.logging.store

import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

trait LogConsumer {
  def process(): Either[String, List[Record]]
  def close(): Unit
  def commit(): Unit
}

// NOTE: copy of com.counterfly.logging.filter.TerminalWord
case class Record(word: String, count: Int)

object KafkaLogConsumer {
  def apply(
    consumerProperties: Properties,
    inputTopic: String,
    pollDurationMs: Duration = Duration.ofMillis(100),
  ): LogConsumer = new LogConsumer {
    private val logger = LoggerFactory.getLogger(getClass)
    println(s" TODO: consumerProperties: $consumerProperties")
    // Create the Kafka consumer
    private val consumer =
      new KafkaConsumer[String, com.counterfly.logging.filter.FilterService.TerminalWord](consumerProperties)
    consumer.subscribe(java.util.Collections.singletonList(inputTopic))

    override def process(): Either[String, List[Record]] = {
      logger.info("Starting Kafka consumer")
      logger.info(s"  Subscribed to topic: $inputTopic")
      consume
    }

    override def close(): Unit = {
      logger.info("Kafka consumer closing")
      consumer.close()
    }

    override def commit: Unit = consumer.commitSync()

    private def consume: Either[String, List[Record]] = {
      val records = consumer.poll(pollDurationMs)
      logger.debug(s"Polled ${records.count()} records")

      if (records.isEmpty) {
        Left("no records found")
      } else {
        // Process the records
        logger.info(s"Found ${records.count()} records")
        val recordList = records.asScala.toList.map { record =>
          val terminalWord = record.value()
          logger.info(s"Received record: key=${record.key()}, value=${terminalWord}")
          // TODO: Send to store service
          println(s"""
                  | -------
                  | Received record: ${record}
                  | TW: ${terminalWord}
                  | -------""".stripMargin)

          Record(terminalWord.word, terminalWord.count)
        }
        Right(recordList)
      }
    }
  }
}
