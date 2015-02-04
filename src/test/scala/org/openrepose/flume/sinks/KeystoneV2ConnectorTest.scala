package org.openrepose.flume.sinks

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class KeystoneV2ConnectorTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with JettyTestServer {
  val keystoneHandler = new KeystoneV2Handler
  setHandler(keystoneHandler)

  override def beforeAll() {
    startServer()
  }

  describe("generateToken") {
    it("should send an invalid payload and handle a 4xx response") {
      val keystoneV2Connector = new KeystoneV2Connector(s"http://localhost:$localPort", "failtest", "", Map())
      val token = keystoneV2Connector.getToken

      token shouldBe a[Failure[_]]
    }
    it("should send a valid payload and receive a valid token for the user provided") {
      val keystoneV2Connector = new KeystoneV2Connector(s"http://localhost:$localPort", "usr", "pwd", Map())
      val token = keystoneV2Connector.getToken

      token shouldBe a[Success[_]]
      token.get should equal("tkn-id")
    }
    it("should cache a token until invalidated") {
      val keystoneV2Connector = new KeystoneV2Connector(s"http://localhost:$localPort", "usr", "pwd", Map())
      val goodToken = keystoneV2Connector.getToken

      goodToken shouldBe a[Success[_]]
      goodToken.get should equal("tkn-id")
      keystoneHandler.numberOfInteractions should equal(1)

      val sameToken = keystoneV2Connector.getToken

      sameToken shouldBe a[Success[_]]
      sameToken.get should equal("tkn-id")
      keystoneHandler.numberOfInteractions should equal(1)

      KeystoneV2Connector.invalidateCachedToken()

      val newToken = keystoneV2Connector.getToken

      newToken shouldBe a[Success[_]]
      newToken.get should equal("tkn-id")
      keystoneHandler.numberOfInteractions should equal(2)
    }
  }

  override def afterEach() {
    keystoneHandler.resetInteractions()
    KeystoneV2Connector.invalidateCachedToken()
  }

  override def afterAll() {
    stopServer()
  }
}
