package org.openrepose.flume.sinks

import java.nio.charset.StandardCharsets

import org.apache.flume.Sink.Status
import org.apache.flume.{Channel, Event, Transaction}
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mockitoEq}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class AtomPublishingSinkTest extends FunSpec with Matchers with MockitoSugar {

  describe("process") {
    it("should commit a successful transaction") {
      val mockChannel = mock[Channel]
      val mockTransaction = mock[Transaction]
      val mockEvent = mock[Event]
      val mockKeystoneConnector = mock[KeystoneV2Connector]
      val mockFeedPublisher = mock[CloudFeedPublisher]
      when(mockKeystoneConnector.getToken).thenReturn("tkn")
      when(mockChannel.getTransaction).thenReturn(mockTransaction)
      when(mockChannel.take).thenReturn(mockEvent)
      when(mockEvent.getBody).thenReturn("tst bdy".getBytes(StandardCharsets.UTF_8))

      val sink = new AtomPublishingSink()
      sink.setChannel(mockChannel)
      sink.keystoneV2Connector = mockKeystoneConnector
      sink.feedPublisher = mockFeedPublisher

      val status = sink.process()

      status should be theSameInstanceAs Status.READY
      verify(mockFeedPublisher, times(1)).publish(mockitoEq("tst bdy"), mockitoEq("tkn"))
      verify(mockTransaction, times(1)).begin()
      verify(mockTransaction, times(1)).commit()
      verify(mockTransaction, times(1)).close()
    }
    it("should rollback an unsuccessful transaction") {
      val mockChannel = mock[Channel]
      val mockTransaction = mock[Transaction]
      val mockEvent = mock[Event]
      val mockKeystoneConnector = mock[KeystoneV2Connector]
      val mockFeedPublisher = mock[CloudFeedPublisher]
      when(mockKeystoneConnector.getToken).thenReturn("tkn")
      when(mockFeedPublisher.publish(mockitoEq("tst bdy"), mockitoEq("tkn"))).thenThrow(new RuntimeException())
      when(mockChannel.getTransaction).thenReturn(mockTransaction)
      when(mockChannel.take).thenReturn(mockEvent)
      when(mockEvent.getBody).thenReturn("tst bdy".getBytes(StandardCharsets.UTF_8))

      val sink = new AtomPublishingSink()
      sink.setChannel(mockChannel)
      sink.keystoneV2Connector = mockKeystoneConnector
      sink.feedPublisher = mockFeedPublisher

      val status = sink.process()

      status should be theSameInstanceAs Status.BACKOFF
      verify(mockFeedPublisher, times(1)).publish(mockitoEq("tst bdy"), mockitoEq("tkn"))
      verify(mockTransaction, times(1)).begin()
      verify(mockTransaction, times(1)).rollback()
      verify(mockTransaction, times(1)).close()
    }
  }
}
