package org.openrepose.flume.sinks

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class AtomPublisherTest extends FunSpec with Matchers {

  describe("pack") {
    it("wraps some content string in an Atom envelope") {
      val atomContent = AtomPublisher.pack("""{"key":"value"}""")

      atomContent should fullyMatch regex """<entry xmlns="http://www.w3.org/2005/Atom"><id>tag:example.org,2007:/foo/entries/1</id><title type="text">User Access Event</title><author><name>Repose</name></author><updated>\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d.\d\d\dZ</updated><published>\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d.\d\d\dZ</published><content type="text">\{"key":"value"\}</content></entry>""".r
    }
  }
}
