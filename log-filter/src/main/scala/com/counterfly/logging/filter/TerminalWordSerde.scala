package com.counterfly.logging.filter

import FilterService.TerminalWord
import java.nio.charset.StandardCharsets
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

class TerminalWordSerializer extends Serializer[FilterService.TerminalWord] {
  override def serialize(topic: String, data: TerminalWord): Array[Byte] = {
    if (data == null) return null
    Try {
      val json = s"""{
        "count":${data.count},
        "word":"${data.word}"
      }"""
      json.getBytes(StandardCharsets.UTF_8)
    } match {
      case Success(value) => value
      case Failure(e) => throw new SerializationException("Error serializing TerminalWord", e)
    }
  }

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
  override def close(): Unit = {}
}

class TerminalWordDeserializer extends Deserializer[FilterService.TerminalWord] {

  private implicit val formats: DefaultFormats.type = DefaultFormats

  override def deserialize(topic: String, data: Array[Byte]): FilterService.TerminalWord = {
    if (data == null) return null
    Try {
      val jsonStr = new String(data, StandardCharsets.UTF_8)

      // Parse JSON using json4s
      val json = parse(jsonStr)
      val word = (json \ "word").extractOpt[String] match {
        case None => throw new RuntimeException("FilterService.TerminalWord missing 'word'")
        case Some(id) => id
      }

      val count = (json \ "count").extractOpt[Int] match {
        case Some(i) => i
        case None => throw new RuntimeException("FilterService.TerminalWord missing 'count'")
      }

      FilterService.TerminalWord(word, count)
    } match {
      case Success(value) => value
      case Failure(e) => throw e
    }
  }

  override def close(): Unit = {
    // No resources to release
  }
}

object TerminalWordSerde {
  def apply(): Serde[TerminalWord] =
    new Serde[TerminalWord] {
      override def serializer(): Serializer[TerminalWord] = new TerminalWordSerializer
      override def deserializer(): Deserializer[TerminalWord] = new TerminalWordDeserializer
      override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}
      override def close(): Unit = {}
    }
}
