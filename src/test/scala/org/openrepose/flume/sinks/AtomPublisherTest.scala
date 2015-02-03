package org.openrepose.flume.sinks

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class AtomPublisherTest extends FunSpec with Matchers {

  describe("pack") {
    it("wraps some content string in an Atom envelope") {
      val atomContent = AtomPublisher.pack("""{"key":"value"}""")

      atomContent should equal("""<entry xmlns="http://www.w3.org/2005/Atom"><id>tag:example.org,2007:/foo/entries/1</id><title type="text">User Access Event</title><summary type="html">&lt;p>A user access event recorded by Repose.&lt;/p></summary><author><name>Repose</name></author><updated>2015-02-03T23:33:28.624Z</updated><published>2015-02-03T23:33:28.624Z</published><content type="text">{"key":"value"}</content></entry>""")
    }
  }
}
