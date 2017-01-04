package org.yashsriv

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route

import com.typesafe.config.Config

import org.yashsriv.api.{ Batch, Movie }
import org.yashsriv.helpers.Worker

trait HttpService extends Batch with Movie with Worker {

  def config: Config
  val logger: LoggingAdapter

  def requestUri(implicit config: Config): Uri = Uri.Empty
    .withScheme(config.getString("services.omdb-api.scheme"))
    .withHost(config.getString("services.omdb-api.host"))
    .withPort(config.getInt("services.omdb-api.port"))

  def routes(implicit config: Config): Route = {
    logRequestResult("akka-http-batch-api") {
      batchApi(api) ~ api
    }
  }

  def api(implicit config: Config): Route = movieApi(requestUri) // ~ Someother api and so on

}
