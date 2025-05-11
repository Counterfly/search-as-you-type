package com.counterfly.logging.filter

import java.nio.charset.StandardCharsets
import java.time.Instant
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 *  String encoding defaults to UTF8 and can be customized by setting the property key.deserializer.encoding,
 *  value.deserializer.encoding or deserializer.encoding. The first two take precedence over the last.
 */
class LogMessageSerializer extends Serializer[LogMessage] {
  override def serialize(topic: String, data: LogMessage): Array[Byte] = {
    if (data == null) return null
    Try {
      val json =
        s"""{"id":"${data.id}","timestamp":"${data.timestamp}","message":"${data.message}"}"""
      json.getBytes(StandardCharsets.UTF_8)
    } match {
      case Success(logMessage) => logMessage
      case Failure(e) =>
        throw new SerializationException("Error serializing LogMessage", e)
    }
  }

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}

class LogMessageDeserializer extends Deserializer[LogMessage] {
  private implicit val formats: DefaultFormats.type = DefaultFormats

  override def deserialize(topic: String, data: Array[Byte]): LogMessage = {
    if (data == null) return null

    Try {
      val jsonStr = new String(data, StandardCharsets.UTF_8)
      // Parse JSON using json4s
      val json = parse(jsonStr)
      val id = (json \ "id").extractOpt[String] match {
        case None => throw new RuntimeException("LogMessage missing 'id'")
        case Some(id) => id
      }

      val timestamp = (json \ "timestamp").extractOpt[String] match {
        case Some(ts) => Instant.parse(ts)
        case None => (json \ "timestamp").extractOpt[Long] match {
            case Some(ts) => Instant.ofEpochMilli(ts)
            case None => throw new RuntimeException("LogMessage missing 'timestamp'")
          }
      }

      val log = (json \ "message").extractOpt[String] match {
        case Some(msg) => msg
        case None => throw new RuntimeException("LogMessage missing 'message'")
      }

      LogMessage(id, timestamp, log)
    } match {
      case Success(logMessage) => logMessage
      case Failure(e) => throw e
    }
  }

  override def close(): Unit = {
    // No resources to release
  }
}

object LogMessageSerde {
  def apply(): Serde[LogMessage] =
    new Serde[LogMessage] {
      override def serializer(): Serializer[LogMessage] = new LogMessageSerializer
      override def deserializer(): Deserializer[LogMessage] = new LogMessageDeserializer
      override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
      override def close(): Unit = {}
    }
}
