package org.yashsriv.api

import scala.concurrent.Future

import akka.NotUsed
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{ Sink, Source }

import org.yashsriv.helpers.Worker
import org.yashsriv.json.BatchSupport

trait Batch extends Directives with BatchSupport with SprayJsonSupport with Worker {

  /**
    * Endpoint for Batch Http Requests
    */
  def batchApi(route: Route): Route =
    pathPrefix("batch") {
      pathEndOrSingleSlash {
        post {
          entity(as[List[Future[HttpRequest]]]) { list ⇒
            complete { batch(route)(list) }
          }
        }
      }
    }

  /**
    * Convert a list of `Future[HttpRequest]` to a `Source` of HttpRequests and
    * pass it through a Route to obtain Response
    */
  def batch(route: Route)(requests: List[Future[HttpRequest]]) = {
    val source: Source[HttpRequest, NotUsed] = requests.map(Source.fromFuture(_)).fold(Source.empty)(_ ++ _)
    val flow = Route.handlerFlow(route)
    val sink = Sink.fold[List[HttpResponse], HttpResponse](List())(_ :+ _)
    source via flow runWith(sink)
  }

}
