package org.openrepose.flume.sinks

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap, FunSpec, Matchers}

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class KeystoneV2ConnectorTest extends FunSpec with BeforeAndAfterAllConfigMap with Matchers {
  val testServer = new Server(0)
  testServer.setHandler(new KeystoneV2Handler())

  var localPort = -1
  var keystoneV2Connector: KeystoneV2Connector = _

  override def beforeAll(configMap: ConfigMap) {
    testServer.start()
    localPort = testServer.getConnectors()(0).asInstanceOf[ServerConnector].getLocalPort
    keystoneV2Connector = new KeystoneV2Connector(s"http://localhost:$localPort")
  }

  describe("generateToken") {
    it("should send a valid payload and receive a valid token for the user provided") {
      val token = keystoneV2Connector.generateToken("usr", "pwd")

      token shouldBe a[Success[_]]
      token.get should equal("tkn-id")
    }
  }

  override def afterAll(configMap: ConfigMap) {
    testServer.stop()
  }

  class KeystoneV2Handler extends AbstractHandler {
    override def handle(target: String,
                        baseRequest: Request,
                        request: HttpServletRequest,
                        response: HttpServletResponse): Unit = {
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType)
      response.setStatus(HttpStatus.SC_OK)
      baseRequest.setHandled(true)
      response.getWriter.print("{\"access\":{\"token\":{\"id\":\"tkn-id\"}}}")
    }
  }

}
