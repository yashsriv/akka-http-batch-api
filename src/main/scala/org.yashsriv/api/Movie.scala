package org.yashsriv.api

import java.io.IOException

import scala.concurrent.Future

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ HttpResponse, HttpRequest }
import akka.http.scaladsl.model.StatusCodes.{ OK, BadRequest }
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal

import spray.json._

import org.yashsriv.helpers.Worker
import org.yashsriv.json.MovieSupport
import org.yashsriv.models.MovieQuery
import org.yashsriv.models.MovieResult

trait Movie extends Directives with Worker with MovieSupport with SprayJsonSupport {

  def omdbApiRequest(request: HttpRequest): Future[HttpResponse] = Http().singleRequest(request)

  def movieApi(implicit requestUri: Uri): Route = pathPrefix("movie") {
    (post & entity(as[MovieQuery])) { movieQuery ⇒
      complete {
        fetchMovieInfo(movieQuery).map[ToResponseMarshallable] {
          case Right(movieInfo) => movieInfo
          case Left(errorMessage) => BadRequest → errorMessage
        }
      }
    }
  }

  def fetchMovieInfo(mq: MovieQuery)(implicit requestUri: Uri): Future[Either[String, MovieResult]] = {
    omdbApiRequest(RequestBuilding.Get(requestUri withQuery convertToQuery(mq))).flatMap { response =>
      response.status match {
        case OK         ⇒ Unmarshal(response.entity).to[MovieResult].map(Right(_))
        case BadRequest ⇒ Future.successful(Left(s"${mq.toJson.prettyPrint} \nIncorrect Movie Format"))
        case _          ⇒ Unmarshal(response.entity).to[String].flatMap { entity ⇒
          val error = s"Omdb request failed with status code ${response.status} and entity $entity"
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
