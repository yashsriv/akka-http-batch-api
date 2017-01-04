package org.yashsriv

import scala.concurrent.ExecutionContextExecutor

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object AkkaHttpBatchApi extends App with HttpService {

  override implicit val system: ActorSystem = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer: Materializer = ActorMaterializer()

  implicit val config: Config = ConfigFactory.load()
  override implicit val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

}
