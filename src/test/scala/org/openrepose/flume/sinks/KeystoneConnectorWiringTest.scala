package org.openrepose.flume.sinks

import org.apache.flume.Context
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.collection.JavaConverters._
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class KeystoneConnectorWiringTest extends FunSpec with BeforeAndAfterAll with Matchers with JettyTestServer {
  val keystoneHandler = new KeystoneV2Handler
  setHandler(new KeystoneV2Handler)

  override def beforeAll() {
    startServer()
  }

  describe("sink configure and connector getToken") {
    it("should construct a Keystone v2 connector with provided parameters from the context") {
      val context = new Context(Map("identity.endpoint" -> s"http://localhost:$localPort",
                                    "identity.username" -> "usr",
                                    "identity.password" -> "pwd").asJava)
      val sink = new AtomPublishingSink
      sink.configure(context)

      val token = sink.keystoneV2Connector.getToken

      token shouldBe a[Success[_]]
      token.get should equal("tkn-id")
    }
  }

  override def afterAll() {
    stopServer()
  }
}
