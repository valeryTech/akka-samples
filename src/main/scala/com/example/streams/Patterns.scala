package com.example.streams

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

object Patterns extends App {

  implicit val system: ActorSystem             = ActorSystem("reactive-tweets")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Our example

  // Writing the batch to a database is most likely an asynchronous operation
  def writeBatchToDatabase(batch: Seq[Int]): Future[Unit] =
    Future {
      println(s"Writing batch of $batch to database by ${Thread.currentThread().getName}")
      Thread.sleep(1000)
    }

  private val ids = Source(1 to 30)
  ids
    .grouped(10)
    .mapAsync(10)(writeBatchToDatabase)
    .runWith(Sink.ignore)


  val printIntegers = Source(1 to 10)
    .throttle(1, 1.second)
    .groupedWithin(3, 2.second)
    .runForeach(v => println(s"${DateTime.now.toString}: $v"))

  printIntegers.onComplete(_ => system.terminate())


  // By default run in one thread. In fact A, B and C don’t have their own thread but share a common pool.

  def stage(name: String): Flow[Int, Int, NotUsed] =
    Flow[Int].map { index =>
      println(s"Stage $name processing $index by ${Thread.currentThread().getName}")
      index
    }

  // async computation
  // concurrent computation
  // Throttling
  // Error handling and recovery
  // Akka-stream now provides exponential backoff recovery.
  // ?recoverWithRetries

  val done = Source(1 to 30)
    .watchTermination() { (_, done) =>
      done.onComplete {
        case Success(_)     => println("Stream completed successfully")
        case Failure(error) => println(s"Stream failed with error $error")
      }
    }
    .throttle(elements = 1, per = 2.second, maximumBurst = 10, ThrottleMode.shaping)  // groupedWithin
    .idleTimeout(5.seconds)
    .via(stage("A"))
    .async
    .via(stage("B"))
    .async
    .via(stage("C"))
    .async
    .runWith(Sink.ignore)

    .recover {
      case _: TimeoutException =>
        println("No messages received for 30 seconds")
    }

//
//  done.onComplete(_ => sys.terminate())
//
}
