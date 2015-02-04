package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class KeystoneV2ConnectorTest extends FunSpec with BeforeAndAfterAllConfigMap with BeforeAndAfterEach with Matchers with JettyTestServer {
  val keystoneHandler = new KeystoneV2Handler
  setHandler(keystoneHandler)

  override def beforeAll(configMap: ConfigMap) {
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

  override protected def afterEach(): Unit = {
    keystoneHandler.resetInteractions()
    KeystoneV2Connector.invalidateCachedToken()
  }

  override def afterAll(configMap: ConfigMap) {
    stopServer()
  }

  class KeystoneV2Handler extends AbstractHandler {
    var numberOfInteractions: Int = 0

    def resetInteractions() = {
      numberOfInteractions = 0
    }

    override def handle(target: String,
                        baseRequest: Request,
                        request: HttpServletRequest,
                        response: HttpServletResponse): Unit = {
      numberOfInteractions += 1
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
