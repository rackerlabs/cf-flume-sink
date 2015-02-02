package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import java.util.Date

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpHeaders, HttpStatus}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.{Success, Try}

/**
 * A utility class that enables communication with a Keystone V2 Identity service.
 */
object KeystoneV2Connector {
  private final val TOKENS_ENDPOINT = "/v2.0/tokens"

  // note: Reads/writes to this static field are not atomic. As a result, multiple requests may be made to the Identity
  //       service when this field has expired. An AtomicReference may be used to enforce stricter threading policies.
  private var cachedToken: Option[String] = None
}

class KeystoneV2Connector(identityHost: String) {

  import org.openrepose.flume.sinks.KeystoneV2Connector._

  def getToken(username: String, password: String): Try[String] = {
    cachedToken match {
      case Some(id) =>
        Success(id)
      case _ =>
        Try(requestIdentityToken(username, password))
    }
  }

  def invalidateCachedToken(): Unit = {
    cachedToken = None
  }

  private def requestIdentityToken(username: String, password: String): String = {
    val httpClient = HttpClients.createDefault()
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
            cachedToken = Some(parseTokenFromJson(responseEntity.getContent, responseEntity.getContentLength))
            cachedToken.get
          } else {
            throw new Exception("Response from the identity service was not in JSON format as expected")
          }
        case _ =>
          throw new Exception("Could not retrieve a token from the identity service")
      }
    } finally {
      EntityUtils.consume(httpResponse.getEntity)
      httpResponse.close()
    }
  }

  private def parseTokenFromJson(tokenStream: InputStream, contentLength: Long): String = {
    implicit lazy val jsonFormats = org.json4s.DefaultFormats

    val contentBuffer = new Array[Byte](contentLength.toInt) // note: unsafe conversion for large values of contentLength
    new DataInputStream(tokenStream).readFully(contentBuffer)
    val contentString = new String(contentBuffer)

    (parse(contentString) \ "access" \ "token" \ "id").extract[String]
  }
}

case class Token(id: String, expiration: Date)
