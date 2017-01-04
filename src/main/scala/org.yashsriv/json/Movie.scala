package org.yashsriv.json

import scala.util.Try

import akka.http.scaladsl.model.Uri.Query

import spray.json._

import org.yashsriv.models._
import QueryType._, Plot._

trait MovieSupport extends DefaultJsonProtocol {

  // Result Format
  implicit val movieResultJsonFormat = jsonFormat(MovieResult, "Title", "Year", "Released",
                                                  "Director", "Plot", "imdbRating", "imdbId",
                                                  "Response", "Error")

  implicit object QueryTypeFormat extends RootJsonFormat[QueryType] {
    def write(qt: QueryType): JsString = JsString(qt.toString)
    def read(value: JsValue): QueryType = value match {
      case JsString("movie")   ⇒ movie
      case JsString("episode") ⇒ episode
      case JsString("series")  ⇒ series
      case _                   ⇒ throw new DeserializationException("QueryType Expected")
    }
  }

  implicit object PlotFormat extends RootJsonFormat[Plot] {
    def write(plot: Plot): JsString = JsString(plot.toString)
    def read(value: JsValue): Plot = value match {
      case JsString("short") ⇒ short
      case JsString("full")  ⇒ full
      case _                 ⇒ throw new DeserializationException("Plot Expected")
    }
  }

  implicit val movieQueryJsonFormat = jsonFormat(MovieQuery, "t", "type", "y", "plot")

  def convertToQuery(mq: MovieQuery): Query = Query(
    "t" → mq.title,
    "type" → mq.qType.getOrElse(movie).toString,
    "year" → mq.year.getOrElse("").toString,
    "plot" → mq.plot.getOrElse(short).toString
  )

}
