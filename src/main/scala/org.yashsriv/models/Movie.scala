package org.yashsriv.models

import QueryType._
import Plot._

case class MovieQuery (
  title: String,
  qType: Option[QueryType],
  year: Option[Int],
  plot: Option[Plot.Plot]
)

case class MovieResult (
  title: Option[String],
  year: Option[String],
  released: Option[String],
  director: Option[String],
  plot: Option[String],
  imdbRating: Option[String],
  imdbId: Option[String],
  response: String,
  error: Option[String]
)
