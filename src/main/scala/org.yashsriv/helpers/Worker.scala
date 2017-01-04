package org.yashsriv.helpers

import scala.concurrent.ExecutionContextExecutor

import akka.actor.ActorSystem
import akka.stream.Materializer

trait Worker {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
}
