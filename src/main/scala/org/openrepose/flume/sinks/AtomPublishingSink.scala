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
                                                  context.getSubProperties("identity.http.properties.").asScala.toMap)
    feedPublisher = new CloudFeedPublisher(context.getString("feeds.endpoint"),
                                           context.getSubProperties("feeds.http.properties.").asScala.toMap)
  }

  override def process(): Status = {
    val channel = getChannel
    val txn = channel.getTransaction

    txn.begin()
    try {
      val event = channel.take()

      feedPublisher.publish(new String(event.getBody), keystoneV2Connector.getToken)

      txn.commit()
      Status.READY
    } catch {
      case t: Throwable =>
        txn.rollback()

        t match {
          case er: Error =>
            logger.error("An error occurred, event could not be processed", er)
            throw er
          case ue: UnauthorizedException =>
            keystoneV2Connector.invalidateCachedToken()
            logger.warn(s"Event could not be processed at this time: ${ue.getMessage}")
          case _ =>
            logger.warn(s"Event could not be processed at this time: ${t.getMessage}")
        }

        Status.BACKOFF
    } finally {
      txn.close()
    }
  }
}
