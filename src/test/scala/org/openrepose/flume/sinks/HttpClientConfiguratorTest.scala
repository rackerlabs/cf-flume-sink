package org.openrepose.flume.sinks

import org.scalatest.{FunSpec, Matchers}

class HttpClientConfiguratorTest extends FunSpec with Matchers {

  describe("buildClient") {
    ignore("should return a new HttpClient configured appropriately") {
      val clientProps = Map("connection.request.timeout" -> "1000",
                            "cookie.spec" -> "ignoreCookies",
                            "redirects.enabled" -> "false")

      val httpClient = HttpClientConfigurator.buildClient(clientProps)

      // todo: test client
    }
  }
}
