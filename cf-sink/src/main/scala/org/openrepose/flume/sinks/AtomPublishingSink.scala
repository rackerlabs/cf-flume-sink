package org.openrepose.flume.sinks

import com.typesafe.scalalogging.LazyLogging
import org.apache.flume.Context
import org.apache.flume.Sink.Status
import org.apache.flume.conf.Configurable
import org.apache.flume.sink.AbstractSink

import scala.collection.JavaConverters._

/**
 * A custom Flume sink for publishing to an endpoint using the AtomPub protocol.
 */
class AtomPublishingSink extends AbstractSink with Configurable with LazyLogging {

  var keystoneV2Connector: KeystoneV2Connector = _
  var feedPublisher: CloudFeedPublisher = _

  override def configure(context: Context): Unit = {
    keystoneV2Connector = new KeystoneV2Connector(context.getString("identity.endpoint"),
                                                  context.getString("identity.username"),
                                                  context.getString("identity.password"),
                                                  context.getSubProperties("identity.properties.http.").asScala.toMap)
    feedPublisher = new CloudFeedPublisher(context.getString("feeds.endpoint"),
                                           context.getSubProperties("feeds.properties.http.").asScala.toMap)
  }

  override def process(): Status = {
    def failureMessage(ex: Exception) = logger.warn(s"Event could not be processed at this time: ${ex.getMessage}")

    val channel = getChannel
    val txn = channel.getTransaction

    txn.begin()
    try {
      Option(channel.take()) match {
        case Some(event) =>
          feedPublisher.publish(AtomFormatter.wrap(new String(event.getBody)), keystoneV2Connector.getToken)

          txn.commit()
          Status.READY
        case None =>
          logger.trace("Process method triggered, but no events could be pulled off the channel")
          txn.rollback()
          Status.BACKOFF
      }
    } catch {
      case t: Throwable =>
        txn.rollback()

        t match {
          case er: Error =>
            logger.error("An error occurred, event could not be processed", er)
            throw er
          case ue: UnauthorizedException =>
            keystoneV2Connector.invalidateCachedToken()
            failureMessage(ue)
          case _ =>
            failureMessage _
        }

        Status.BACKOFF
    } finally {
      txn.close()
    }
  }
}
