package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.{Request, Server, ServerConnector}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CloudFeedPublisherTest extends FunSpec with BeforeAndAfterAllConfigMap with BeforeAndAfterEach with Matchers with JettyTestServer{
  val feedsHandler = new FeedsHandler
  setHandler(feedsHandler)
  var publisher: CloudFeedPublisher = _

  override def beforeAll(configMap: ConfigMap) {
    startServer()
  }

  override protected def beforeEach() = {
    publisher = new CloudFeedPublisher(s"http://localhost:$localPort", Map())
  }

  describe("publish") {
    it("should post to endpoint and do nothing on success") {
      publisher.publish("foo", "bar")

      feedsHandler.interaction.method should equal("POST")
      feedsHandler.interaction.tokenHeader should equal("bar")
      feedsHandler.interaction.body should equal("foo")
    }

    it("should post to endpoint and throw an exception when unauthorized") {
      feedsHandler.returnStatus = HttpStatus.SC_UNAUTHORIZED
      intercept[UnauthorizedException]  {
        publisher.publish("foo", "bar")
      }
    }

    val statusCodes = List(HttpStatus.SC_BAD_GATEWAY, HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_FORBIDDEN, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_REQUEST_TOO_LONG)
    statusCodes.foreach { statusCode =>
      it(s"should post to endpoint and throw an exception for status $statusCode") {
        feedsHandler.returnStatus = statusCode
        intercept[Exception] {
          publisher.publish("foo", "bar")
        }
      }
    }
  }

  override protected def afterEach() = {
    feedsHandler.resetInteractions()
  }

  override def afterAll(configMap: ConfigMap) {
    stopServer()
  }

  class FeedsHandler extends AbstractHandler {
    var interaction: RequestInteraction = _
    var returnStatus = HttpStatus.SC_OK

    def resetInteractions() = {
      returnStatus =HttpStatus.SC_OK
      interaction = null
    }

    override def handle(target: String,
                        baseRequest: Request,
                        request: HttpServletRequest,
                        response: HttpServletResponse): Unit = {
      interaction = RequestInteraction(request.getMethod, request.getHeaders("X-AUTH-TOKEN").nextElement().toString, readBody(request.getInputStream, request.getContentLength))
      response.setContentType(ContentType.APPLICATION_JSON.getMimeType)
      response.setStatus(returnStatus)
      baseRequest.setHandled(true)
      response.getWriter.print( """{"foo":"bar"}""")
    }

    private def readBody(contentStream: InputStream, contentLength: Long) = {
      val contentBuffer = new Array[Byte](contentLength.toInt) // note: unsafe conversion for large values of contentLength
      new DataInputStream(contentStream).readFully(contentBuffer)
      new String(contentBuffer)
    }
  }

  case class RequestInteraction(method: String, tokenHeader: String, body: String)
}
