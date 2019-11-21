package com.example

import akka.actor.{Actor, ActorSystem, Props}

object StartStopActor1 {
  def props: Props =
    Props(new StartStopActor1)
}

class StartStopActor1 extends Actor {
  override def preStart(): Unit = {
    println("first started")
    context.actorOf(StartStopActor2.props, "second")
  }

  override def postStop(): Unit = println("first stopped")

  override def receive: Receive = {
    case "stop" => context.stop(self)
  }
}

object StartStopActor2 {
  def props: Props =
    Props(new StartStopActor2)
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("second started")

  override def postStop(): Unit = println("second stopped")

  // Actor.emptyBehavior is a useful placeholder when we don't
  // want to handle any messages in the actor.
  override def receive: Receive = Actor.emptyBehavior
}


object ActorLyfecycle {
  def main(args: Array[String]): Unit = {

    // Create the 'helloAkka' actor system
    val system: ActorSystem = ActorSystem("helloAkka")

    val first = system.actorOf(StartStopActor1.props, "first")
    first ! "stop"
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

object SupervisingActor {
  def props: Props = Props(new SupervisingActor)
}

class SupervisingActor extends Actor {
  val child = context.actorOf(SupervisedActor.props, "supervised-actor")

  override def receive: Receive = {
    case "failChild" => child ! "fail"
  }
}

object SupervisedActor {
  def props: Props = Props(new SupervisedActor)
}

class SupervisedActor extends Actor {

  override def preStart(): Unit = println("supervised actor started")

  override def postStop(): Unit = println("supervised actor stopped")

  override def receive: Receive = {
    case "fail" =>
      println("supervised actor fails now")
      throw new Exception("I failed!")
  }
}

object SupervisingActorLifeCycle {
  def main(args: Array[String]): Unit = {
    // Create the 'helloAkka' actor system
    val system: ActorSystem = ActorSystem("helloAkka")

    val supervisingActor = system.actorOf(SupervisingActor.props, "supervising-actor")
    supervisingActor ! "failChild"
  }
}