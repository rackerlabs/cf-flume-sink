package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

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
