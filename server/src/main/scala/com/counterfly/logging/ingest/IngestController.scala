package com.counterfly.logging.ingest

import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class IngestController(
  ingestService: IngestService,
) extends Controller {

  private val PathPrefix = "v1"
  def path(path: String): String = s"/$PathPrefix/$path"

  post(path("log")) { request: Request =>
    // Log the message
    val logMessage = request.contentString
    logger.info(s"Received log entry: $logMessage")

    ingestService
      .log(logMessage)
      .map { _ =>
        response.ok
      }
  }
}
