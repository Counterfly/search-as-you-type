package com.counterfly.logging.filter

import java.time.Instant

object LogMessageUtils {
  def generateLogMessages(message: String): Seq[LogMessage] = {
    val sb = new StringBuilder
    var count = 0
    for {
      char <- message.toCharArray
    } yield {
      count += 1
      sb.append(char)
      LogMessage(
        s"$count",
        Instant.now(),
        sb.toString(),
      )
    }
  }
}
