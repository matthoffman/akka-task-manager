package org.mhoffman

;

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.{Dispatchers}
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode}
import com.eaio.uuid.UUID
import collection.immutable.{HashMap, Queue}
import java.util.ArrayList


/**
 * Description defaults to name if it's not set.
 *
 * Putting everything a user might want to add in the in the constructor, so users can use named & default parameters
 *
 * In Java, I'd do this with a builder pattern. In Scala 2.8, it seems that case classes and named & default parameters
 * are the cleanest way to do this:
 * http://villane.wordpress.com/2010/03/05/taking-advantage-of-scala-2-8-replacing-the-builder/
 *
 * And the examples of "typesafe builder patterns in Scala" that Google returned were abysmally ugly.
 *
 *
 *
 * Perhaps TaskContext is what's handed into Workers, and "addChild" sends a message back to the service, which in turn
 * sends a message back to the taskContext telling it to add a local task -- so the TaskContext (or task within) is really
 * a local eventually-consistent copy of the authoritative TaskGraph's children.
 *
 * So, where are the actors?  Is the context itself an actor?  Is the context itself stored as a local variable by an actor,
 * and mutated? (treating actor == thread)
 *
 * The Task (or TaskGraphNode) might actually be an actor... since it's in charge of state that can mutate, it seems it
 * should be represented as an actor. 
 * 
 */
case class TaskDefinition(

        /**
         * The name we'll use to identify this task.
         * Must be unique; we'll use this name to refer to the task and look it up from registries
         */
        name: String,

        /**Optional, but can describe this task */
        description: String = name,

        /**
         * Currently, this is just the name of a worker.  There are certainly more elegant, typesafe ways to do this --
         * I'm open to suggestions.  The string is here because we've used Spring to store workers in the past, so
         * this string is the name of the relevant Spring bean. */
        worker: String,

        /**
         * A map of properties to send to this task.  For example, "shard_id = 01" or "file_to_process = foo.gz".
         * Will be interpreted by the worker, and usually conveys interesting information about what to do, e.g.
         * pointers to the data to be processed.
         * Really, there's nothing keeping us from using non-strings here, except that it becomes very tempting to put
         * large objects here, which then get serialized and send over the wire... and that leads to a lot more overhead
         * than developers were thinking of.
         * However, since Akka has a serialization framework, perhaps we should revisit this restriction.
         */
        properties: Map[String, String] = new HashMap[String, String],

        /**
         * Every task has a parent, but by default the parent will be the root
         */
        parent: TaskDefinition,

        /**
         * In case this taskDefinition is defined with  children off-the-bat. We can add them later during runtime.
         */
        children: List[TaskDefinition] = new List[TaskDefinition]) {
  // that was all the constructor. Yeesh.

  // TODO: Look up the parent as the root if it's null


  /**
   * always created on initialization; no reason to expose it as an option
   */
  val taskDefinitionId: UUID = new UUID()

}

/**
 * A runtime instance of a task, represented by an Akka actor.
 *
 * I'm mixing terms here.  I use "executing" and "checked out" interchangeably.  
 * I tend to think of Tasks as something that a client "checks out" and "checks in", kind of like SCM.  In those terms,
 * it could actually go from ready -> executing -> ready -> executing, and so on.
 * Eventually it hits a terminal state: "failed" or "complete".
 *
 * In this implementation, we're only letting the task be checked out by one person at a time.  That's for convenience,
 * really... there's nothing really stopping us from allowing speculative execution.  We'd just have to track the parallel
 * executions separately, either within this actor or perhaps as linked actors (each current execution getting its own actor).
 * Not sure if there's value in that approach? 
 */
class Task(val taskDefinition: TaskDefinition) extends Actor {

  /*
   * The lifecycle of a task is potentially circular.  It goes like this:
   *
   * ready <---> executing -> succeeded
   *               |
   *        (failed...retry?) --> back to ready
   *                         \--> failed  
   *
   * For this quick implementation, I'm ignoring retries.  I'd like to piggyback the Fault Tolerance features of Akka,
   * but I haven't reasoned that out yet. 
   */
  
  /**
   * Add a child task. Only possible when the task is checked out, but we're ensuring that through the "becomes" state-transition construct.
   */
  def addChild() = {


  }

  /**
   * When the task is executing, we can add children or mark the task
   */
  def checkedOut(): Receive = {
    case (checkin)  => // check this task in
    case (addChild) => // add a new child task

  }

  def receive = {

  }

  def startTask: Receive = {
    // someone is executing the task
    case ("foo") => become(stopTask)
  }

  def stopTask: Receive = {
    // the task is now finished.
    case ("bar") => become(startTask)
  }

  // TODO: if more than one person is executing the task, do they each get separate Actors?
  // Or does the Actor track the simultaneous executions?
  // Or, perhaps, does a Task actor supervise multiple TaskExecution actors? 

}

/*
 * Persistent datastructures
 *
 * Akka's STM should only be used with immutable data. This can be costly if you have large datastructures and are using a naive copy-on-write. In order to make working with immutable datastructures fast enough Scala provides what are called Persistent Datastructures. There are currently two different ones:
 *
 *     * HashMap (scaladoc)
 *     * Vector (scaladoc)
 *
 *
 * They are immutable and each update creates a completely new version but they are using clever structural sharing in order to make them almost as fast, for both read and update, as regular mutable datastructures.
 */

