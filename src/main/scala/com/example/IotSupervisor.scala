package com.example

import akka.actor.{Actor, ActorLogging, Props}

object IotSupervisor {
  def props(): Props = Props(new IotSupervisor)
}

class IotSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("IoT Application started")

  override def postStop(): Unit = log.info("IoT Application stopped")

  // No need to handle any messages
  override def receive = Actor.emptyBehavior
}

import akka.actor.ActorSystem
import scala.io.StdIn

object IotApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("iot-system")

    try {

      // Create top level supervisor
      val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")

      // Exit the system after ENTER is pressed
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}