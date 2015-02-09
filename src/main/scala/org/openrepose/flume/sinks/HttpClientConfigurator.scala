package org.openrepose.flume.sinks

import java.net.InetAddress

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

object HttpClientConfigurator extends LazyLogging {

  def buildClient(httpProperties: Map[String, String]): CloseableHttpClient = {
    val requestConfigBuilder = RequestConfig.custom()

    httpProperties foreach {
      case ("authentication.enabled", value) => requestConfigBuilder.setAuthenticationEnabled(value.toBoolean)
      case ("circular.redirects.allowed", value) => requestConfigBuilder.setCircularRedirectsAllowed(value.toBoolean)
      case ("connection.request.timeout", value) => requestConfigBuilder.setConnectionRequestTimeout(value.toInt)
      case ("connect.timeout", value) => requestConfigBuilder.setConnectTimeout(value.toInt)
      case ("cookie.spec", value) => requestConfigBuilder.setCookieSpec(value)
      case ("expect.continue.enabled", value) => requestConfigBuilder.setExpectContinueEnabled(value.toBoolean)
      case ("local.address", value) => requestConfigBuilder.setLocalAddress(InetAddress.getByName(value))
      case ("max.redirects", value) => requestConfigBuilder.setMaxRedirects(value.toInt)
      case ("proxy", value) => requestConfigBuilder.setProxy(new HttpHost(value))
      // case ("proxy.preferred.auth.schemes", value) => requestConfigBuilder.setProxyPreferredAuthSchemes()
      case ("redirects.enabled", value) => requestConfigBuilder.setRedirectsEnabled(value.toBoolean)
      case ("relative.redirects.allowed", value) => requestConfigBuilder.setRelativeRedirectsAllowed(value.toBoolean)
      case ("socket.timeout", value) => requestConfigBuilder.setSocketTimeout(value.toInt)
      case ("stale.connection.check.enabled", value) => requestConfigBuilder.setStaleConnectionCheckEnabled(value.toBoolean)
      // case ("target.preferred.auth.schemas", value) => requestConfigBuilder.setTargetPreferredAuthSchemes()
      case (key, _) => logger.error( s"""Could not set the HttpClient "$key" property""")
    }

    HttpClients.custom().setDefaultRequestConfig(requestConfigBuilder.build()).build()
  }
}
