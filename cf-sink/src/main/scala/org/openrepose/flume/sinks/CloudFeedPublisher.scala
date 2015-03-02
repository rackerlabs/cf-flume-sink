package org.openrepose.flume.sinks

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

import scala.io.{Codec, Source}

/**
 * A simple class to publish events to Cloud Feeds.
 */
class CloudFeedPublisher(feedsEndpoint: String, httpProperties: Map[String, String]) extends LazyLogging {

  private val httpClient = HttpClients.custom()
    .setDefaultRequestConfig(HttpClientConfigurator.getConfig(httpProperties))
    .build()

  def publish(atomMessage: String, identityToken: String): Unit = {
    logger.debug("Attempting to publish to cloud feeds")

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
          val responseBody = Source.fromInputStream(httpResponse.getEntity.getContent)(Codec.UTF8).mkString
          throw new Exception(s"""Feeds rejected the post with status code: $statusCode, body: $responseBody""")
      }
    } finally {
      EntityUtils.consume(httpResponse.getEntity)
      httpResponse.close()
    }
  }
}

class UnauthorizedException(message: String) extends Exception
