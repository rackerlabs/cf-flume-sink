package org.openrepose.flume.sinks

import org.eclipse.jetty.server.{Handler, Server, ServerConnector}

trait JettyTestServer {
  private val _testServer = new Server(0)
  private var _localPort = -1

  def localPort = _localPort

  def startServer(): Unit = {
    _testServer.start()
    _localPort = _testServer.getConnectors()(0).asInstanceOf[ServerConnector].getLocalPort
  }

  def setHandler(handler: Handler): Unit = {
    _testServer.setHandler(handler)
  }

  def stopServer(): Unit = {
    _testServer.stop()
    _localPort = -1
  }
}
