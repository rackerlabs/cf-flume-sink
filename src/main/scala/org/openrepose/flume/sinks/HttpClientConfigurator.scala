package org.openrepose.flume.sinks

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.BasicHttpParams

import scala.util.Try

object HttpClientConfigurator extends LazyLogging {

  def buildClient(httpProperties: Map[String, String]): HttpClient = {
    val httpClientParams = new BasicHttpParams()

    // todo: what happens when a long is desired, but the value fits into an int?
    httpProperties foreach { case (key, value) =>
      Try(value.toInt).map(httpClientParams.setIntParameter(key, _)).orElse(
      Try(value.toLong).map(httpClientParams.setLongParameter(key, _)).orElse(
      Try(value.toDouble).map(httpClientParams.setDoubleParameter(key, _)).orElse(
      Try(value.toBoolean).map(httpClientParams.setBooleanParameter(key, _)).orElse(
      Try(httpClientParams.setParameter(key, value)).recover {
        case _ => logger.error(s"""Could not set the HttpClient "$key" property""")}))))
    }

    new DefaultHttpClient(httpClientParams)
  }
}
