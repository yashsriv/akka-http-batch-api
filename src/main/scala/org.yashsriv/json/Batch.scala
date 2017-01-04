package org.yashsriv.json

import scala.concurrent.Future
import scala.util.Try

import argonaut._, Argonaut._

import akka.http.scaladsl.marshalling.{ Marshal, ToResponseMarshaller, Marshalling, ToEntityMarshaller, Marshaller }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, HttpResponse, HttpEntity, HttpMethod, HttpMethods }
import akka.http.scaladsl.model.{ MessageEntity, Uri, RequestEntity }
import akka.stream.scaladsl.{ Source, Sink }
import akka.util.ByteString

import org.yashsriv.helpers.Worker

trait BatchSupport extends Worker {

  /**
   * Marshalling and Unmarshalling of HttpHeader as
   * {
   *   "name": "Header Name",
   *   "value": "Header Value"
   * }
   */
  implicit def httpHeaderCodecJson: CodecJson[HttpHeader] =
    CodecJson(
      (header: HttpHeader) ⇒
      HttpHeader.unapply(header) match {
        case Some((n, v)) ⇒ Json("name" := n, "value" := v)
        case None         ⇒ jEmptyObject
      },
      c ⇒ for {
        name ← (c --\ "name").as[String]
        value ← (c --\ "value").as[String]
      } yield HttpHeader.parse(name, value) match {
        case HttpHeader.ParsingResult.Ok(h, _) ⇒ h
        case _                                 ⇒ RawHeader("", "")
      }
    )

  /**
   *  Returns corresponding HttpMethod from string
   */
  def getMethod(method: String): HttpMethod = method match {
    case "GET"     ⇒ HttpMethods.GET
    case "CONNECT" ⇒ HttpMethods.CONNECT
    case "DELETE"  ⇒ HttpMethods.DELETE
    case "HEAD"    ⇒ HttpMethods.HEAD
    case "OPTIONS" ⇒ HttpMethods.OPTIONS
    case "PATCH"   ⇒ HttpMethods.PATCH
    case "POST"    ⇒ HttpMethods.POST
    case "PUT"     ⇒ HttpMethods.PUT
    case "TRACE"   ⇒ HttpMethods.TRACE
    case _         ⇒ HttpMethod.custom(method)
  }


  /**
   * Unmarshalling of HttpRequest from
   * {
   *   "method": "GET/PUT/POST/PATCH/DELETE...etc"
   *   "relative_url": "Relative url for our api endpoint"
   *   "headers": "An array of headers as json"
   *   "body": "JSON stringified body for post requests"
   * }
   */
  implicit def futureHttpRequestDecodeJson: DecodeJson[Future[HttpRequest]] =
    DecodeJson(
      c ⇒ for {
        method ← (c --\ "method").as[String]
        relativeUrl ← (c --\ "relative_url").as[String]
        headers ← (c --\ "headers").as[Option[List[HttpHeader]]]
        body ← (c --\ "body").as[Option[String]]
      } yield for {
        res ← Marshal(body).to[MessageEntity]
        json = res.withContentType(`application/json`)
        req = HttpRequest(getMethod(method), Uri(relativeUrl), headers.getOrElse(List()), json)
      } yield req
    )

  /**
    * Marshal a List[HttpResponse] to an HttpResponse like:
    * {
    *   "code": 200
    *   "headers": [
    *     {
    *       "name": "Content-Type"
    *       "value": "application/json"
    *     }
    *   ]
    *   "body": "{\"data\": \"Blah\"}"
    * }
    */
  implicit def httpResponseListMarshal: ToEntityMarshaller[List[HttpResponse]] =
    Marshaller { implicit ec ⇒ (responses: List[HttpResponse]) ⇒

      // Sink for folding Source of ByteString into 1 single huge ByteString
      val sink = Sink.fold[ByteString, ByteString](ByteString.empty)(_ ++ _)

      // A List of Future Json obtained by folding Source[ByteString]
      // and mapping appropriately
      val listFuture: List[Future[Json]] = for {
        res ← responses
      } yield for {
        byteString ← res._3.dataBytes runWith sink
        string = byteString.utf8String
      } yield ("code" := res._1.intValue) ->:
        ("headers" := res._2.toList) ->:
        ("body" := string) ->: jEmptyObject


      // Convert List[Future[Json]] to Future[List[Json]]
      val futureList: Future[List[Json]] = Future.sequence(listFuture)

      // ToEntityMarshaller is essentially a Future[List[Marshalling[RequestEntity]]]
      for {
        list ← futureList
        json = jArray(list).nospaces
      } yield List(
        Marshalling.Opaque[RequestEntity](() ⇒
          HttpEntity(`application/json`, json)
        ).asInstanceOf[Marshalling[RequestEntity]]
      )
    }

}
