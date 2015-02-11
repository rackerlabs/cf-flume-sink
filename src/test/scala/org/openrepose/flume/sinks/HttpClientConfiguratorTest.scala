package org.openrepose.flume.sinks

import org.scalatest.{FunSpec, Matchers}

class HttpClientConfiguratorTest extends FunSpec with Matchers {

  describe("buildClient") {
    it("should return a new RequestConfig configured appropriately") {
      val clientProps = Map("connectionRequestTimeout" -> "1000",
                            "cookieSpec" -> "ignoreCookies",
                            "redirectsEnabled" -> "false")

      val reqConf = HttpClientConfigurator.getConfig(clientProps)

      reqConf.getConnectionRequestTimeout shouldBe 1000
      reqConf.getCookieSpec shouldBe "ignoreCookies"
      reqConf.isRedirectsEnabled shouldBe false
    }
  }
}
