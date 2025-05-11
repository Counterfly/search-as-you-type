package com.counterfly.logging.filter

import java.time.Duration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Suppressed
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.kstream.KStream
import org.apache.kafka.streams.scala.kstream.KTable
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

trait Processor {
  def process(): Unit

  def close(): Unit
}

// TODO: move to separate file, bring in LogMessageDeserializer
object KafkaProcessor {
  def apply(
    streamProperties: StreamsConfig,
    inputTopic: String,
    outputTopic: String,
    // Window configuration
    // TODO: should be configurable
    windowSizeMs: Long = 10000, // 10 seconds window size
    windowHopMs: Long = 9000, // 9 seconds hop interval
  ): Processor = new Processor {
    private val WindowGapWithHopMs = windowSizeMs - windowHopMs
    require(WindowGapWithHopMs > 0, s"Window size must be greater than hop size: $windowSizeMs > $windowHopMs")

    private val logger = LoggerFactory.getLogger(getClass)

    // TODO: remove implicit
    implicit val logMessageSerde = LogMessageSerde()
    implicit val terminalWordSerde = TerminalWordSerde()

    // Define the stream processing logic
    // Create a stream of from topic, with the key as a string and the value as a LogMessage
    // and is windowed every 10 seconds
    // and advanced by 9 seconds (hopping window setup)
    // TODO: better scaling with localized groupings (for e.g. by first letter of the log message)
    private val inputStream: KafkaStreams = {
      val hoppingWindow =
        TimeWindows.ofSizeAndGrace(
          Duration.ofMillis(10.seconds.toMillis),
          Duration.ofMillis(9.seconds.toMillis),
        )

      val builder = new StreamsBuilder()
      val inputStream: KStream[String, LogMessage] = builder
        .stream[String, LogMessage](
          inputTopic,
          Consumed.`with`(
            Serdes.String(),
            logMessageSerde,
          ),
        )

      val windowedStream: KTable[Windowed[Integer], java.util.List[LogMessage]] =
        inputStream
          // This makes a global aggregation, not good for performance but sufficient for now
          .groupBy({ case (_, _) => Integer.valueOf(0) })(
            Grouped.`with`(
              Serdes.Integer(),
              logMessageSerde,
            ),
          )
          .windowedBy(hoppingWindow)
          .aggregate(
            (new java.util.ArrayList[LogMessage]()).asInstanceOf[java.util.List[LogMessage]],
          )((_: Integer, logMessage: LogMessage, agg: java.util.List[LogMessage]) => {
            // TODO: aggregate using Trie
            agg.add(logMessage)
            agg
          })(
            Materialized.`with`(
              Serdes.Integer(),
              Serdes.ListSerde(classOf[java.util.ArrayList[LogMessage]], logMessageSerde),
            ),
          )
          // Suppress intermediate updates until the window closes
          .suppress(
            Suppressed.untilWindowCloses(BufferConfig.unbounded()),
          )

      // apply filter logic and output to a new stream
      windowedStream
        .toStream
        .map { case (windowedKey, logMessages) =>
          val windowStart = windowedKey.window().start()
          val windowEnd = windowedKey.window().end()

          logger.info(
            s"Window Closed [${windowStart} ${windowEnd}), messages: ${logMessages.size()}",
          )
          // TODO: use Trie
          val terminalWords: java.util.List[FilterService.TerminalWord] =
            new java.util.ArrayList[FilterService.TerminalWord]()

          (s"global_$windowStart_$windowEnd", terminalWords)
        }
        // output terminal words to a new topic
        .to(outputTopic)(Produced.`with`(
          Serdes.String(),
          Serdes.ListSerde(classOf[java.util.ArrayList[FilterService.TerminalWord]], terminalWordSerde),
        ))

      new KafkaStreams(builder.build(), streamProperties)
    }

    override def process(): Unit = {
      logger.info("Starting Kafka stream processing")
      logger.info(s"  Window size: ${windowSizeMs}ms, Window hop: ${windowHopMs}ms")
      logger.info(s"  Subscribed to topic: $inputTopic. Outputting to topic: $outputTopic")

      inputStream.start()
      logger.info("Kafka stream processing started")
    }

    override def close(): Unit = {
      inputStream.close()
    }
  }
}
