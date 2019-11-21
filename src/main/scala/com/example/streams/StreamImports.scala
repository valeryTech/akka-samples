package com.example.streams

trait StreamImports {
  import akka.NotUsed
  import akka.actor.ActorSystem
  import akka.stream.scaladsl.{Flow, Sink, Source}
  import akka.stream.{ActorMaterializer, ThrottleMode}
  import org.joda.time.DateTime

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.{Future, TimeoutException}
  import scala.util.{Failure, Success}
  import sys._

  implicit val system: ActorSystem             = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

}
