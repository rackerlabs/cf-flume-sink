package org.openrepose.flume.sinks

import org.apache.flume.Context
import org.apache.flume.Sink.Status
import org.apache.flume.conf.Configurable
import org.apache.flume.sink.AbstractSink

/**
 * A custom Flume sink for publishing to an endpoint using the AtomPub protocol.
 */
class AtomPublishingSink extends AbstractSink with Configurable {
  override def configure(context: Context): Unit = ???

  override def process(): Status = {
    var status: Status = null

    // Start transaction
    val channel = getChannel
    val txn = channel.getTransaction
    txn.begin()
    try {
      // This try clause includes whatever Channel operations you want to do

      val event = channel.take()

      // Send the Event to the external repository.
      // storeSomeData(e)

      txn.commit()
      status = Status.READY
    } catch {
      case t: Throwable =>
        txn.rollback()

        // Log exception, handle individual exceptions as needed

        status = Status.BACKOFF

        // re-throw all Errors
        t match {
          case error: Error =>
            throw error
          case _ =>
        }
    } finally {
      txn.close()
    }
    status
  }
}
