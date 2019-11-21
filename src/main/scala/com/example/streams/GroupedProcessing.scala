package com.example.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.example.streams.Patterns.system
import org.joda.time.DateTime

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object GroupedProcessing extends App with StreamImports {

  val printIntegers = Source(1 to 10)
    .throttle(1, 1.second)
    .groupedWithin(3, 2.second)
    .runForeach(v => println(s"${DateTime.now.toString}: $v"))

  printIntegers.onComplete(_ => system.terminate())

}
