package org.openrepose.flume.sinks

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils

/**
 * A simple class to publish events to Cloud Feeds.
 */
class CloudFeedPublisher(feedsEndpoint: String, httpProperties: Map[String, String]) extends LazyLogging {
  private val httpClient = HttpClientConfigurator.buildClient(httpProperties)

  def publish(atomMessage: String, identityToken: String): Unit = {
    val httpPost = new HttpPost(feedsEndpoint)
    httpPost.addHeader("X-AUTH-TOKEN", identityToken)
    httpPost.setEntity(new StringEntity(atomMessage, ContentType.APPLICATION_ATOM_XML))
    val httpResponse = httpClient.execute(httpPost)
    try {
      val statusCode = httpResponse.getStatusLine.getStatusCode
      statusCode match {
        case HttpStatus.SC_OK =>
          logger.debug("Successfully published to cloud feeds")
        case HttpStatus.SC_UNAUTHORIZED =>
          throw new UnauthorizedException("Feeds rejected the post as unauthorized")
        case _ =>
          throw new Exception(s"Feeds rejected the post with $statusCode")
      }
    } finally {
      EntityUtils.consume(httpResponse.getEntity)
      httpResponse.close()
    }
  }
}

class UnauthorizedException(message: String) extends Exception
