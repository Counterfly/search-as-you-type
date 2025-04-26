package com.counterfly.logging.ingest

import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.util.Future

class IngestService {
  def log(logMessage: String): Future[Unit] = {
    // TODO: produce request.log to Kafka Stream
    Future.Unit
  }
}
