package org.openrepose.flume.sinks

import java.util.{Date, UUID}

import com.typesafe.scalalogging.LazyLogging
import org.apache.abdera.Abdera
import org.apache.abdera.model.Content

object AtomFormatter extends LazyLogging {

  private val abdera = new Abdera()

  def wrap(content: String): String = {
    logger.debug("Attempting to marshal message into atom")
    logger.trace(content)

    val now = new Date()

    val entry = abdera.newEntry
    entry.setId(UUID.randomUUID().toString)
    entry.setTitle("User Access Event")
    entry.addAuthor("Repose")
    entry.setUpdated(now)
    entry.setContent(content, Content.Type.XML)

    entry.toString
  }
}
