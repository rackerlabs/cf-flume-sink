package org.openrepose.flume.sinks

import org.apache.http.client.params.ClientPNames._
import org.apache.http.client.params.CookiePolicy._
import org.apache.http.params.CoreConnectionPNames._
import org.scalatest.{FunSpec, Matchers}

class HttpClientConfiguratorTest extends FunSpec with Matchers {

  describe("buildClient") {
    it("should return a new HttpClient configured appropriately") {
      val clientProps = Map(CONNECTION_TIMEOUT -> "1000",
                            COOKIE_POLICY -> IGNORE_COOKIES,
                            HANDLE_REDIRECTS -> "false")

      val httpClient = HttpClientConfigurator.buildClient(clientProps)

      httpClient.getParams.getIntParameter(CONNECTION_TIMEOUT, 0) shouldBe 1000
      httpClient.getParams.getParameter(COOKIE_POLICY) shouldBe IGNORE_COOKIES
      httpClient.getParams.getBooleanParameter(HANDLE_REDIRECTS, true) shouldBe false
    }
  }
}
