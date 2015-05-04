package org.openrepose.flume.sinks

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class AtomFormatterTest extends FunSpec with Matchers {

  describe("pack") {
    it("wraps some content string in an Atom envelope") {
      val atomContent = AtomFormatter.wrap("""<key xmlns="test">value</key>""")

      atomContent should fullyMatch regex """<entry xmlns="http://www.w3.org/2005/Atom"><id>.+</id><title type="text">User Access Event</title><author><name>Repose</name></author><updated>\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d.\d\d\dZ</updated><content type="application/xml"><key xmlns="test">value</key></content></entry>""".r
    }
  }
}
