package org.example

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import Actor.{actorOf}
import se.scalablesolutions.akka.dispatch.{Dispatchers}

import collection.immutable.Queue

case class State(value: Int)

trait ComputationType
case object Add extends ComputationType
case object Multiply extends ComputationType

trait Message
case class Change(state: State) extends Message
case class ComputationRequest(computationType: ComputationType, input: Int) extends Message
case class ComputationResult(computationType: ComputationType, input: Int, state: Option[State], result: Option[Int]) extends Message

abstract class Computer(f: (Int, Int) => Int) extends Actor {

  // Lets pretend these are big computations. Give each Computer it's own thread.
  self.dispatcher = Dispatchers.newThreadBasedDispatcher(self)

  var state: Option[State] = None

  def compute(in: Int): Option[Int] = state.map(s => f(s.value, in))

  def receive = {
    case Change(newState) => state = Some(newState)
    case ComputationRequest(t, in) => self reply ComputationResult(t, in, state, compute(in))
  }

}

class AddingComputer extends Computer(_ + _)
class MultiplyingComputer extends Computer(_ * _)

class ComputationRequestQueue(val computer: ActorRef) {
  private var queue = Queue.empty[ComputationRequest]
  private var busy = false

  def add(in: ComputationRequest): Option[ComputationRequest] = 
    if (busy) {
      queue = queue.enqueue(in)
      None
    } else {
      busy = true
      Some(in)
    }

  def next: Option[ComputationRequest] =
    if (!queue.isEmpty) {
      val (r,q) = queue.dequeue
      queue = q
      Some(r)
    } else {
      busy = false
      None
    }
}

class StateManager extends Actor {
  var state: Option[State] = None
  val computerQueues: Map[ComputationType, ComputationRequestQueue] =
    Map(Add -> new ComputationRequestQueue(actorOf[AddingComputer].start), 
        Multiply -> new ComputationRequestQueue(actorOf[MultiplyingComputer].start))

  def receive = {
    case c @ Change(s) =>
      state = Some(s)
      computerQueues.values.foreach(_.computer ! c)

    case cr @ ComputationRequest(t, _) =>
      computerQueues.get(t).foreach(c => c.add(cr).foreach(c.computer ! _))
      
    case ComputationResult(t, i, s, r) =>
      computerQueues.get(t).foreach(c => c.next.foreach(c.computer ! _))
      println("Results of '"+t+"' with '" + i + "' and '" + s + "' is '" + r + "'")
  }
}

object Main {
  def main(args: Array[String]) {
    val computers = actorOf[StateManager].start
    val inputs = List(1,2,3,4,5,6,7,8,9,0)
    inputs.foreach(i => computers ! ComputationRequest(Add, i))
    inputs.foreach(i => computers ! ComputationRequest(Multiply, i))
    computers ! Change(State(5))
    inputs.foreach(i => computers ! ComputationRequest(Add, i))
    inputs.foreach(i => computers ! ComputationRequest(Multiply, i))
    computers ! Change(State(0))
    inputs.foreach(i => computers ! ComputationRequest(Add, i))
    inputs.foreach(i => computers ! ComputationRequest(Multiply, i))
    computers ! Change(State(10))
    inputs.foreach(i => computers ! ComputationRequest(Add, i))
    inputs.foreach(i => computers ! ComputationRequest(Multiply, i))
  }
}
