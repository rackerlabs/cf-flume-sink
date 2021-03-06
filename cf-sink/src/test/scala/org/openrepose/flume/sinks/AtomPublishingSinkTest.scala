package org.openrepose.flume.sinks

import java.nio.charset.StandardCharsets

import org.apache.flume.Sink.Status
import org.apache.flume.{Channel, Event, Transaction}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{anyString, eq => mockitoEq}
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.xml.XML

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
      verify(mockFeedPublisher, times(1)).publish(anyString(), mockitoEq("tkn"))
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
      when(mockFeedPublisher.publish(anyString(), mockitoEq("tkn"))).thenThrow(new RuntimeException())
      when(mockChannel.getTransaction).thenReturn(mockTransaction)
      when(mockChannel.take).thenReturn(mockEvent)
      when(mockEvent.getBody).thenReturn("tst bdy".getBytes(StandardCharsets.UTF_8))

      val sink = new AtomPublishingSink()
      sink.setChannel(mockChannel)
      sink.keystoneV2Connector = mockKeystoneConnector
      sink.feedPublisher = mockFeedPublisher

      val status = sink.process()

      status should be theSameInstanceAs Status.BACKOFF
      verify(mockFeedPublisher, times(1)).publish(anyString(), mockitoEq("tkn"))
      verify(mockTransaction, times(1)).begin()
      verify(mockTransaction, times(1)).rollback()
      verify(mockTransaction, times(1)).close()
    }
    it("should rollback when there are no events in the channel") {
      val mockChannel = mock[Channel]
      val mockTransaction = mock[Transaction]
      when(mockChannel.getTransaction).thenReturn(mockTransaction)
      when(mockChannel.take).thenReturn(null)

      val sink = new AtomPublishingSink()
      sink.setChannel(mockChannel)

      val status = sink.process()

      status should be theSameInstanceAs Status.BACKOFF
      verify(mockTransaction, times(1)).begin()
      verify(mockTransaction, times(1)).rollback()
      verify(mockTransaction, times(1)).close()
    }
    it("should not modify or escape XML content pulled off the channel") {
      val mockChannel = mock[Channel]
      val mockTransaction = mock[Transaction]
      val mockEvent = mock[Event]
      val mockKeystoneConnector = mock[KeystoneV2Connector]
      val mockFeedPublisher = mock[CloudFeedPublisher]
      val postBodyCaptor = ArgumentCaptor.forClass(classOf[String])
      when(mockKeystoneConnector.getToken).thenReturn("tkn")
      when(mockChannel.getTransaction).thenReturn(mockTransaction)
      when(mockChannel.take).thenReturn(mockEvent)
      when(mockEvent.getBody).thenReturn("<event xmlns=\"test\">test</event>".getBytes(StandardCharsets.UTF_8))

      val sink = new AtomPublishingSink()
      sink.setChannel(mockChannel)
      sink.keystoneV2Connector = mockKeystoneConnector
      sink.feedPublisher = mockFeedPublisher

      val status = sink.process()

      status should be theSameInstanceAs Status.READY
      verify(mockFeedPublisher, times(1)).publish(postBodyCaptor.capture(), mockitoEq("tkn"))
      (XML.loadString(postBodyCaptor.getValue) \ "content" \ "event").text shouldBe "test"
    }
  }
}
