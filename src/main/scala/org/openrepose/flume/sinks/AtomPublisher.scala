package org.openrepose.flume.sinks

import java.util.Date

import org.apache.abdera.Abdera
import org.apache.abdera.model.Entry
import org.apache.abdera.protocol.client.{AbderaClient, ClientResponse}

import scala.util.Try

class AtomPublisher {
  private final val abdera = new Abdera()
  private final val abderaClient = new AbderaClient(abdera)

  def pack(content: String): Entry = {
    val feed = abdera.newFeed()

    feed.setId("tag:example.org,2007:/foo")
    feed.setTitle("Test Feed")
    feed.setSubtitle("Feed subtitle")
    feed.setUpdated(new Date())
    feed.addAuthor("James Snell")
    feed.addLink("http://example.com")
    feed.addLink("http://example.com/foo", "self")

    val entry = feed.addEntry()
    entry.setId("tag:example.org,2007:/foo/entries/1")
    entry.setTitle("Entry title")
    entry.setSummaryAsHtml("<p>This is the entry title</p>")
    entry.setUpdated(new Date())
    entry.setPublished(new Date())
    entry.addLink("http://example.com/foo/entries/1")

    entry
  }

  def post(entry: Entry): Try[ClientResponse] = {
    ???
  }
}
