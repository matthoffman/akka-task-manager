package org.mhoffman


import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import se.scalablesolutions.akka.actor.Actor._

/**
 *
 *
 */

class TaskGraph extends Actor {

  def receive = {
    case _ => log.info("Got something")
  }

}