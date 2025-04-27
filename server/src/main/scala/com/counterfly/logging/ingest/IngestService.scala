package com.counterfly.logging
package ingest

import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.logging.Logger
import com.twitter.util.Future
import com.twitter.util.Promise
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import java.time.Instant
import java.util.Properties
import java.util.UUID
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer

class IngestService(
  // TODO: should have an abstraction over KafkaProducer
  producer: KafkaProducer[String, String],
  topic: String, // "log_received"
) {
  private val logger = Logger(getClass) // TODO: use a logger trait

  def log(logMessage: String): Future[Unit] = {
    val messageId = UUID.randomUUID().toString
    // could use a case class for the message, butn it's so simple that leaving it as a string is fine
    val message = s"""{"id":"$messageId","timestamp":"${Instant.now().toString}","message":"$logMessage"}"""

    logger.info(s"Sending log message to Kafka: $message")

    // could use FuturePool to perform Kafka operations off the main thread
    // but producing a message is already async, so no benefit
    val promise = Promise[Unit]()
    Try {
      val record = new ProducerRecord[String, String](topic, messageId, message)

      producer.send(
        record,
        new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (exception != null) {
              logger.error(s"Failed to send message to Kafka: ${exception.getMessage}", exception)
              promise.setException(exception)
            } else {
              logger.debug(
                s"Message sent to Kafka: topic=${metadata.topic()}, partition=${metadata.partition()}, offset=${metadata.offset()}",
              )
              promise.setValue(())
            }
          }
        },
      )
    } match {
      case Return(_) =>
        // Message sent successfully, no action needed
        ()
      case Throw(e) =>
        logger.error(s"Error sending message to Kafka: ${e.getMessage}", e)
        promise.setException(e)
    }

    promise
  }

  // todo: close the producer when service is shutting down
  def close(): Unit = {
    logger.info(s"closing producer")
    Try(producer.close()) match {
      case Return(_) => logger.info("Kafka producer closed successfully")
      case Throw(e) => logger.error("Failed to close Kafka producer", e)
    }
  }
}

object IngestService {

  def createProducer(config: KafkaProducerConfig): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.ACKS_CONFIG, config.acks)
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, config.maxInFlightRequests)
    new KafkaProducer[String, String](props)
  }
}
