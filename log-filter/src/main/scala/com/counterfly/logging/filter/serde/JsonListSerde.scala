package com.counterfly.logging.filter.serde

import java.nio.charset.StandardCharsets
import java.util.{List => JList}
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.json4s.JArray
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class JsonListSerializer[T](elementSerde: Serializer[T]) extends Serializer[JList[T]] {
  private val logger = LoggerFactory.getLogger(getClass)

  override def serialize(topic: String, data: JList[T]): Array[Byte] = {
    if (data == null) return null

    Try {
      // Convert Java List to Scala List for easier processing
      val scalaList = data.asScala.toList

      // Serialize each element in the list
      val jsonElements = scalaList.map { element =>
        if (element == null) {
          "null"
        } else {
          // Use elementSerde to serialize each element to bytes
          val bytes = elementSerde.serialize(topic, element)
          // Then convert bytes back to string for JSON
          if (bytes == null) {
            "null"
          } else {
            new String(bytes, StandardCharsets.UTF_8)
          }
        }
      }

      // Construct JSON array
      val jsonArray = "[" + jsonElements.mkString(",") + "]"

      jsonArray.getBytes(StandardCharsets.UTF_8)
    } match {
      case Success(value) => value
      case Failure(e) =>
        logger.error("Error serializing list", e)
        throw new SerializationException("Error serializing JSON list", e)
    }
  }

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}

class JsonListDeserializer[T](elementSerde: Deserializer[T]) extends Deserializer[JList[T]] {
  private val logger = LoggerFactory.getLogger(getClass)

  override def deserialize(topic: String, data: Array[Byte]): JList[T] = {
    if (data == null) return null

    Try {
      val jsonStr = new String(data, StandardCharsets.UTF_8)

      println(s"Deserializing JSON list: $jsonStr")
      // Parse the JSON array
      val json = parse(jsonStr)

      // Extract array elements
      json match {
        case JArray(elements) =>
          // For each element in the array, convert back to bytes and deserialize
          val result = new java.util.ArrayList[T]()
          elements.foreach { elem =>
            val elemStr = compact(render(elem))
            val elemBytes = elemStr.getBytes(StandardCharsets.UTF_8)
            val deserializedElem = elementSerde.deserialize(topic, elemBytes)
            if (deserializedElem != null) {
              result.add(deserializedElem)
            }
          }
          result

        case _ =>
          logger.error(s"Expected JSON array, but got: $jsonStr")
          throw new SerializationException(s"Expected JSON array, but got: $jsonStr")
      }
    } match {
      case Success(value) => value
      case Failure(e) =>
        logger.error("Error deserializing JSON list", e)
        throw new SerializationException("Error deserializing JSON list", e)
    }
  }

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}

object JsonListSerde {
  def apply[T](elementSerde: Serde[T]): Serde[JList[T]] =
    new Serde[JList[T]] {
      override def serializer(): Serializer[JList[T]] = new JsonListSerializer[T](elementSerde.serializer())
      override def deserializer(): Deserializer[JList[T]] = new JsonListDeserializer[T](elementSerde.deserializer())
      override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
      override def close(): Unit = {}
    }
}
