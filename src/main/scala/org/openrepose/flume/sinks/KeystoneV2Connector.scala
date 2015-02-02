package org.openrepose.flume.sinks

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
}

class KeystoneV2Connector(identityHost: String) {

  import org.openrepose.flume.sinks.KeystoneV2Connector._

  // note: caching should be done by the caller of generateToken
  def generateToken(username: String, password: String): Try[String] = {
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
          implicit lazy val jsonFormats = org.json4s.DefaultFormats
          val responseEntity = httpResponse.getEntity
          val tokenId = (parse(responseEntity.getContent) \ "access" \ "token" \ "id").extract[String]
          EntityUtils.consume(responseEntity)
          Success(tokenId)
        case HttpStatus.SC_BAD_REQUEST => ???
        case HttpStatus.SC_NOT_FOUND => ???
        case _ => ???
      }
    } finally {
      httpResponse.close()
    }
  }
}
