package com.counterfly.logging.filter

import java.time.Instant

case class LogMessage(
  id: String, // message ID
  timestamp: Instant, // timestamp of the log message
  message: String,
)
