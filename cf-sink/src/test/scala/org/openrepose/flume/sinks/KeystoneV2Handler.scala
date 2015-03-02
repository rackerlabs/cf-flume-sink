package org.openrepose.flume.sinks

import java.io.{DataInputStream, InputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

class KeystoneV2Handler extends AbstractHandler {
  private var _numberOfInteractions = 0

  def numberOfInteractions = _numberOfInteractions

  def resetInteractions() = {
    _numberOfInteractions = 0
  }

  override def handle(target: String,
                      baseRequest: Request,
                      request: HttpServletRequest,
                      response: HttpServletResponse): Unit = {
    _numberOfInteractions += 1
    val requestBody = readBody(request.getInputStream, request.getContentLength.toLong)
    if (requestBody.contains("failtest")) {
      response.sendError(400)
    } else {
      val expirationDate = {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, 1)
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(cal.getTime)
      }
      response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString())
      response.setStatus(HttpServletResponse.SC_OK)
      baseRequest.setHandled(true)
      response.getWriter.print(s"""{"access":{"token":{"expires":"$expirationDate","id":"tkn-id"}}}""")
    }
  }

  private def readBody(contentStream: InputStream, contentLength: Long) = {
    val contentBuffer = new Array[Byte](contentLength.toInt) // note: unsafe conversion for large values of contentLength
    new DataInputStream(contentStream).readFully(contentBuffer)
    new String(contentBuffer)
  }
}
