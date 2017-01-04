package org.yashsriv.json

import scala.concurrent.Future
import scala.util.Try

import akka.http.scaladsl.marshalling.{ Marshal, ToResponseMarshaller, Marshalling, ToEntityMarshaller, Marshaller }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, HttpResponse, HttpEntity, HttpMethod, HttpMethods }
import akka.http.scaladsl.model.{ MessageEntity, Uri, RequestEntity }
import akka.stream.scaladsl.{ Source, Sink }
import akka.util.ByteString

import spray.json._

import org.yashsriv.helpers.Worker

trait BatchSupport extends DefaultJsonProtocol with Worker {

  /**
   * Marshalling and Unmarshalling of HttpHeader as
   * {
   *   "name": "Header Name",
   *   "value": "Header Value"
   * }
   */
  implicit object HttpHeaderJsonFormat extends RootJsonFormat[HttpHeader] {
    def write(hh: HttpHeader): JsObject = HttpHeader.unapply(hh) match {
      case Some((n, v)) ⇒ JsObject("name" → JsString(n), "value" → JsString(v))
      case None         ⇒ JsObject()
    }
    def read(value: JsValue): HttpHeader = value.asJsObject.getFields("name", "value") match {
      case Seq(JsString(name), JsString(value)) ⇒ HttpHeader.parse(name, value) match {
        case HttpHeader.ParsingResult.Ok(h, _) ⇒ h
        case _                                 ⇒ RawHeader("", "")
      }
      case _                                    ⇒ throw new DeserializationException("HttpHeader expected")
    }

  }

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
  implicit object HttpRequestJsonFormat extends RootJsonFormat[Future[HttpRequest]] {

    // We are only interested in the reader
    def write(fhr: Future[HttpRequest]): JsValue = JsNull

    def read(value: JsValue): Future[HttpRequest] = {

      val method: String = value.asJsObject.getFields("method") match {
        case Seq(JsString(m)) ⇒ m
        case _                ⇒ throw new DeserializationException("method field necessary in HttpRequest")
      }

      val relativeUrl: String = value.asJsObject.getFields("relative_url") match {
        case Seq(JsString(r)) ⇒ r
        case _                ⇒ throw new DeserializationException("relative_url field necessary in HttpRequest")
      }

      val headers: Vector[HttpHeader] = value.asJsObject.getFields("headers") match {
        case Seq(JsArray(headers)) ⇒ headers map (_.convertTo[HttpHeader])
        case _                     ⇒ Vector()
      }

      val body: String = value.asJsObject.getFields("body") match {
        case Seq(JsString(b)) ⇒ b
        case _           ⇒ ""
      }

      for {
        res ← Marshal(body).to[MessageEntity]
        json = res.withContentType(`application/json`)
        req = HttpRequest(getMethod(method), Uri(relativeUrl), headers, json)
      } yield req

    }
  }

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

      // A List of Future JsObject obtained by folding Source[ByteString]
      // and mapping appropriately
      val listFuture: List[Future[JsObject]] = for {
        res ← responses
      } yield for {
        byteString ← res._3.dataBytes runWith sink
        string = byteString.utf8String
      } yield JsObject(
        "code" → res._1.intValue.toJson,
        "headers" → res._2.toList.toJson,
        "body" → string.toJson
      )


      // Convert List[Future[JsObject]] to Future[List[JsObject]]
      val futureList: Future[List[JsObject]] = Future.sequence(listFuture)

      // ToEntityMarshaller is essentially a Future[List[Marshalling[RequestEntity]]]
      for {
        list ← futureList
        json = list.toJson.compactPrint
      } yield List(
        Marshalling.Opaque[RequestEntity](() ⇒
          HttpEntity(`application/json`, json)
        ).asInstanceOf[Marshalling[RequestEntity]]
      )
    }

}
