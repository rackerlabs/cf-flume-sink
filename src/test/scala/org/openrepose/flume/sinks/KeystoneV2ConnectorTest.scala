package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAllConfigMap, ConfigMap, FunSpec, Matchers}

import scala.util.{Failure, Success}

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
    it("should send an invalid payload and handle a 4xx response") {
      keystoneV2Connector.invalidateCachedToken()
      val token = keystoneV2Connector.getToken("failtest", "")

      token shouldBe a[Failure[_]]
    }
    it("should send a valid payload and receive a valid token for the user provided") {
      keystoneV2Connector.invalidateCachedToken()
      val token = keystoneV2Connector.getToken("usr", "pwd")

      token shouldBe a[Success[_]]
      token.get should equal("tkn-id")
    }
    it("should cache a token until invalidated") {
      val goodToken = keystoneV2Connector.getToken("usr", "pwd")

      goodToken shouldBe a[Success[_]]
      goodToken.get should equal("tkn-id")

      val badToken = keystoneV2Connector.getToken("failtest", "")

      badToken shouldBe a[Success[_]]
      badToken.get should equal("tkn-id")

      keystoneV2Connector.invalidateCachedToken()

      val worstToken = keystoneV2Connector.getToken("failtest", "")

      worstToken shouldBe a[Failure[_]]
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
      val requestBody = readBody(request.getInputStream, request.getContentLength.toLong)
      if (requestBody.contains("failtest")) {
        response.sendError(400)
      } else {
        val expirationDate = {
          val cal = Calendar.getInstance()
          cal.add(Calendar.DATE, 1)
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(cal.getTime)
        }
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType)
        response.setStatus(HttpStatus.SC_OK)
        baseRequest.setHandled(true)
        response.getWriter.print( s"""{"access":{"token":{"expires":"$expirationDate","id":"tkn-id"}}}""")
      }
    }

    private def readBody(contentStream: InputStream, contentLength: Long) = {
      val contentBuffer = new Array[Byte](contentLength.toInt) // note: unsafe conversion for large values of contentLength
      new DataInputStream(contentStream).readFully(contentBuffer)
      new String(contentBuffer)
    }
  }

}
