package org.openrepose.flume.sinks

import java.io.InputStream

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, HttpStatus}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.{Codec, Source}

/**
 * A utility class that enables communication with a Keystone V2 Identity service.
 */
object KeystoneV2Connector {
  private final val TOKENS_ENDPOINT = "/v2.0/tokens"
}

// note (potential bug): The cachedToken is shared between all instances of this class. If multiple instances are
//                       instantiated with different credentials, the cachedToken may never prove useful.
class KeystoneV2Connector(identityHost: String, username: String, password: String, httpProperties: Map[String, String])
  extends LazyLogging {

  import org.openrepose.flume.sinks.KeystoneV2Connector._

  // todo: Set http client properties
  private val httpClient = HttpClientConfigurator.buildClient(httpProperties)

  private var cachedToken: Option[String] = None

  def invalidateCachedToken(): Unit = {
    cachedToken = None
  }

  def getToken: String = {
    cachedToken match {
      case Some(id) => id
      case _ => requestIdentityToken()
    }
  }

  private def requestIdentityToken(): String = {
    val httpPost = new HttpPost(s"$identityHost$TOKENS_ENDPOINT")
    val requestBody = compact(render(
      "auth" ->
        ("passwordCredentials" ->
          ("username" -> username) ~
          ("password" -> password))))
    httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType)
    httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON))

    val httpResponse = httpClient.execute(httpPost)

    try {
      httpResponse.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK | HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION =>
          val responseEntity = httpResponse.getEntity

          if (ContentType.APPLICATION_JSON.getMimeType.equalsIgnoreCase(responseEntity.getContentType.getValue)) {
            cachedToken = Some(parseTokenFromJson(responseEntity.getContent))
            logger.debug("Successfully fetched and parsed token from identity service")
            cachedToken.get
          } else {
            logger.error("Response from the identity service was not in JSON format as expected")
            throw new Exception("Response from the identity service was not in JSON format as expected")
          }
        case _ =>
          logger.error(s"Failed to retrieve token from the identity service, status code: ${httpResponse.getStatusLine.getStatusCode}")
          throw new Exception("Failed to retrieve a token from the identity service")
      }
    } finally {
      EntityUtils.consume(httpResponse.getEntity)
      httpResponse.close()
    }
  }

  private def parseTokenFromJson(tokenStream: InputStream): String = {
    implicit lazy val jsonFormats = org.json4s.DefaultFormats

    val contentString = Source.fromInputStream(tokenStream)(Codec.UTF8).mkString

    (parse(contentString) \ "access" \ "token" \ "id").extract[String]
  }
}
