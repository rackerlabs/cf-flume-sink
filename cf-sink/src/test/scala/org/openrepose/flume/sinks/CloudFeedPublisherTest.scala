package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CloudFeedPublisherTest extends FunSpec with BeforeAndAfterAllConfigMap with BeforeAndAfterEach with Matchers with JettyTestServer {
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
    val goodStatusCodes = List(HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED,
      HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, HttpStatus.SC_NO_CONTENT, HttpStatus.SC_RESET_CONTENT,
      HttpStatus.SC_PARTIAL_CONTENT, HttpStatus.SC_MULTI_STATUS)
    goodStatusCodes.foreach { statusCode =>
      it(s"should post to endpoint and do nothing on success with status $statusCode") {
        feedsHandler.returnStatus = statusCode
        publisher.publish("foo", "bar")

        feedsHandler.interaction.method should equal("POST")
        feedsHandler.interaction.tokenHeader should equal("bar")
        feedsHandler.interaction.body should equal("foo")
      }
    }

    it("should post to endpoint and throw an exception when unauthorized") {
      feedsHandler.returnStatus = HttpStatus.SC_UNAUTHORIZED
      intercept[UnauthorizedException]  {
        publisher.publish("foo", "bar")
      }
    }

    val badStatusCodes = List(HttpStatus.SC_BAD_GATEWAY, HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_FORBIDDEN,
      HttpStatus.SC_NOT_FOUND, HttpStatus.SC_REQUEST_TOO_LONG)
    badStatusCodes.foreach { statusCode =>
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
    var returnStatus = HttpStatus.SC_CREATED

    def resetInteractions() = {
      returnStatus = HttpStatus.SC_CREATED
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
