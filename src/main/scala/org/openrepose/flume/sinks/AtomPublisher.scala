package org.openrepose.flume.sinks

import java.util.Date

import org.apache.abdera.Abdera

object AtomPublisher {

  private val abdera = new Abdera()

  def pack(content: String): String = {
    val now = new Date()

    val entry = abdera.newEntry
    entry.setId("tag:example.org,2007:/foo/entries/1") // todo: No idea what this ID should be...
    entry.setTitle("User Access Event")
    entry.addAuthor("Repose")
    entry.setUpdated(now)
    entry.setPublished(now)
    entry.setContent(content) // todo: set the content type if text is not desired (needs to be configurable based on template?)

    entry.toString
  }
}
