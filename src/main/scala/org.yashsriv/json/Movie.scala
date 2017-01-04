package org.yashsriv.json

import scala.util.Try

import argonaut._, Argonaut._

import akka.http.scaladsl.model.Uri.Query

import org.yashsriv.models._
import QueryType._, Plot._

trait MovieSupport {

  implicit def queryTypeCodecJson: CodecJson[QueryType] =
    CodecJson(
      (qt: QueryType) ⇒ jString(qt.toString),
      c ⇒ for {
        qType ← c.as[String]
      } yield qType match {
        case "movie"   ⇒ movie
        case "series"  ⇒ series
        case "episode" ⇒ episode
        case _         ⇒ throw new IllegalArgumentException
      }
    )

  implicit def plotCodecJson: CodecJson[Plot] =
    CodecJson(
      (plot: Plot) ⇒ jString(plot.toString),
      c ⇒ for {
        plot ← c.as[String]
      } yield plot match {
        case "short"   ⇒ short
        case "full"    ⇒ full
        case _         ⇒ throw new IllegalArgumentException
      }
    )

  // Result Format
  implicit def movieResultCodecJson: CodecJson[MovieResult] =
    casecodec9(MovieResult.apply, MovieResult.unapply)("Title", "Year", "Released",
                                                      "Director", "Plot", "imdbRating",
                                                      "imdbId", "Response", "Error")

  implicit def movieQueryCodecJson: CodecJson[MovieQuery] =
    casecodec4(MovieQuery.apply, MovieQuery.unapply)("t", "type", "y", "plot")

  def convertToQuery(mq: MovieQuery): Query = Query(
    "t" → mq.title,
    mq.qType match {
      case Some(q) ⇒ "type" → q.toString
      case _       ⇒ "type" → "movie"
    },
    mq.year match {
      case Some(year) ⇒ "y" → year.toString
      case _          ⇒ "y" → ""
    },
    mq.plot match {
      case Some(p) ⇒ "plot" → p.toString
      case _       ⇒ "plot" → "short"
    }
  )

}
