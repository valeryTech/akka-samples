package com.example.streams

import java.lang.System.currentTimeMillis
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import scala.io.StdIn

object ScalaStreamsTest extends App {

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  private val N = 100000000
  val source: Source[Int, NotUsed] = Source(1 to N)

  // val done = source.runForeach(i => println(i))

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  // we can reuse source
  val result: Future[IOResult] =
    factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  factorials.map(_.toString).runWith(lineSink("factorials2.txt"))

  factorials.zipWith(Source(1 to N))((num, idx) => s"$idx! = $num")
    .throttle(1, 1 second)
    .runForeach(println)

  //////////////////////////////////////////////////////////////
  val akkaTag = Hashtag("#akka")
  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), currentTimeMillis, "#akka !") ::
      Tweet(Author("ktosopl"), currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("mmartynas"), currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil
  )

  final case class Author(handle: String)

  tweets
    .map(_.hashtags)                // Get all sets of hashtags ...
    .reduce(_ ++ _)                 // ... and reduce them to a single set, removing duplicates across all tweets
    .mapConcat(identity)            // Flatten the set of hashtags to a stream of hashtags
    .map(_.name.toUpperCase)        // Convert all hashtags to upper case
    .runWith(Sink.foreach(println)) // Attach the Flow to a Sink that will finally print the hashtags

  ///////////////////////////////////////////////////////////////////////////////

  final case class Hashtag(name: String)

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally system.terminate()

//  result.onComplete(_ => system.terminate())
//  println(result)
}
