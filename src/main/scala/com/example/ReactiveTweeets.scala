package com.example

import java.lang.System.currentTimeMillis

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{RunnableGraph, Sink, Source, _}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ReactiveTweeets extends App {

  implicit val system       = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

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

  val authors: Source[Author, NotUsed] =
    tweets.filter(_.hashtags.contains(akkaTag)).map(_.author)
  authors.runWith(Sink.foreach(println))

  val hashtags: Source[Hashtag, NotUsed] = tweets.buffer(4, OverflowStrategy.dropHead).mapConcat(_.hashtags.toList)
  hashtags.runWith(Sink.foreach(println))

  // broadcast
  val writeAuthors: Sink[Author, NotUsed]   = ???
  val writeHashtags: Sink[Hashtag, NotUsed] = ???

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList) ~> writeHashtags
    ClosedShape
  })
  g.run()

  // counts
  val count: Flow[Tweet, Int, NotUsed]         = Flow[Tweet].map(_ => 1)
  val sumSink: Sink[Int, Future[Int]]          = Sink.fold[Int, Int](0)(_ + _)
  val counterGraph: RunnableGraph[Future[Int]] = tweets.via(count).toMat(sumSink)(Keep.right)

  val sum: Future[Int] = counterGraph.run()
  sum.foreach(c => println(s"Total tweets processed: $c"))

  val sumOneLine: Future[Int] = tweets.map(_ => 1).runWith(sumSink)
  sum.foreach(c => println(s"Total tweets processed: $c"))

}
