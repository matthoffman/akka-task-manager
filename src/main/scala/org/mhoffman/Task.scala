package org.mhoffman

;

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.{Dispatchers}
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode}
import com.eaio.uuid.UUID
import collection.immutable.{HashMap, Queue}


/**
 * A TaskDefinition is to a class what a Task is to an object.  That is, TaskDefinition describes a task that could be
 * executed, and a Task is an instance of a TaskDefinition, which is ready to be executed.   There could be many Tasks
 * which are instances of the same TaskDefinition.
 *
 * Putting everything a user might want to add in the in the constructor, so users can use named & default parameters
 *
 * In Java, I'd do this with a builder pattern. In Scala 2.8, it seems that case classes and named & default parameters
 * are the cleanest way to do this ( http://villane.wordpress.com/2010/03/05/taking-advantage-of-scala-2-8-replacing-the-builder/ ).
 * And the examples of "typesafe builder patterns in Scala" that Google returned were abysmally ugly.
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
                                 description: String = "",

                                 /**
                                  * Currently, this is a block that executes something interesting.  In previous implementations I've worked on
                                  * (and in most of the open-source job queues I'm aware of), this is just a string. A block seems interesting,
                                  * and I'm curious to see how it works.
                                  */
                                 worker: (TaskContext) => Unit,

                                 /**
                                  * A map of properties to send to this task.  For example, "shard_id = 01" or "file_to_process = foo.gz".
                                  * Will be interpreted by the worker, and usually conveys interesting information about what to do, e.g.
                                  * pointers to the data to be processed.
                                  * Really, there's nothing keeping us from using non-strings here, except that it becomes very tempting to put
                                  * large objects here, which then get serialized and send over the wire... and that leads to a lot more overhead
                                  * than this framework is designed to handle.
                                  * However, since Akka has a serialization framework, perhaps we should revisit this restriction.
                                  */
                                 properties: Map[String, String] = Map(),

                                 /**
                                  * Removed, because this doesn't really makes sense; with these default parameters, we can't define both the parent and
                                  * the children and ensure that they're consistent.
                                  * That's part of the convenience of the builder pattern I guess -- we can do that kind of thing automatically.
                                  *
                                  * Every task has a parent, but by default the parent will be the root
                                  */
                                 // parent: TaskDefinition = TaskDefinition.ROOT,

                                 executeChildrenInParallel: Boolean = true,

                                 /**
                                  * In case this taskDefinition is defined with children off-the-bat.  If you don't define them up front in the
                                  * task definition, they can still be added later during runtime.
                                  */
                                 children: List[TaskDefinition] = List()) {
  // that was all the constructor. Yeesh.


  /**
   * always created on initialization; no reason to expose it as an option
   */
  val taskDefinitionId: UUID = new UUID()

}

object TaskDefinition {
  val ROOT = TaskDefinition("root", "the root task", {
    context => log.info("Root Task")
  })
}

class TaskContext(val taskActorRef: ActorRef, val myNode: String) {

  def addChild(taskDef: TaskDefinition): Boolean = {
    taskActorRef ! AddChild(myNode, taskDef)
    true;
  }
}

/**
 * A runtime instance of a task, represented by an Akka actor.
 *
 * I'm mixing terms here.  I use "executing" and "checked out" interchangeably.  
 * I tend to think of Tasks as something that a client "checks out" and "checks in", kind of like SCM.  In those terms,
 * it could actually go from ready -> executing -> ready -> executing, and so on.
 * Eventually it hits a terminal state: "failed" or "successful".
 *
 * In this implementation, we're only letting the task be checked out by one person at a time.  That's for convenience,
 * really... there's nothing really stopping us from allowing speculative execution.  We'd just have to track the parallel
 * executions separately, either within this actor or perhaps as linked actors (each current execution getting its own actor).
 * Not sure if there's value in that approach? 
 */

class Task(val taskDefinition: TaskDefinition) extends Actor {

  var node = ""
  // the node that has this task currently checked out. This should be in a persistent map, really, of task executions or something similar.

  var state = Ready

  /**the children defined in the original task definition */
  val taskDefinitionChildren = taskDefinition.children

  // not sure if we want this.
  var children = List[Task]()
  // TODO: should this be ActorRefs or something?  RemoteActorRef?

  var childTaskIds = List[String]()
  //

  var properties: Map[String, String] = taskDefinition.properties

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
   * Add a child task.
   * Right now, this is only possible when the task is checked out -- we're ensuring that through the "becomes" state-transition construct.
   * Conceptually, I'm not necessarily sure we need this restriction, but I haven't reasoned through all the implications
   * of adding a child to task that you haven't checked out. 
   */

  def addChild(taskDefinition: TaskDefinition): Unit = {
    0
  }

  /**
   * When the task is executing, we can add children or mark the task
   */

  def checkedOut(): Receive = {

    case Checkin(requestingNode, executionStatus, properties) => // check this task in
    // if it's successful, become completed
    // if it's not successful, become failed
      log.info("being checked in by " + requestingNode)
      executionStatus match {
        case ExecutionSuccessful() =>
          log.info("execution successful. Moving to complete status")
          become(successful)

        case ExecutionFailed() =>
          log.info("execution failed. Moving to failed status")
          become(failed)

        case ExecutionAborted() =>
          log.info("execution aborted. Moving back to ready status")
          become(readyForCheckout)
      }

    case AddChild(requestingNode: String, newTD: TaskDefinition) => // add a new child task
      log.info(requestingNode + " is adding child " + newTD)
      addChild(newTD)

    case GetChildren(requestingNode: String) =>
      self.reply_?(children)

    case GetTaskInfo(requestingNode: String) =>
      self.reply_?(new TaskInfo(taskDefinition, properties /*, state*/)) //TODO: why is this giving me a type mismatch?

    case GetState(requestingNode: String) => returnState(requestingNode, CheckedOut(node))
    case other => throw new RuntimeException("Don't know what to do with message: " + other)
  }

  def readyForCheckout: Receive = {
    // someone is executing the task
    case Checkout(requestingNode: String) =>
      log.info("being checked out by " + requestingNode)
      this.node = requestingNode
      become(checkedOut)

    case AddChild(requestingNode: String, newTD: TaskDefinition) => // add a new child task
      log.info(requestingNode + " is adding child " + newTD)
      addChild(newTD)

    case GetChildren(requestingNode: String) =>
      self.reply_?(children)

    case GetState(requestingNode: String) => returnState(requestingNode, Ready())
    case other => throw new RuntimeException("Don't know what to do with message: " + other)
  }

  def successful: Receive = {
    // the task is now finished.
    case GetState(requestingNode: String) => returnState(requestingNode, Successful())
    case other => throw new RuntimeException("Don't know what to do with message " + other)
  }

  def failed: Receive = {
    // the task is now finished.
    case GetState(requestingNode: String) => returnState(requestingNode, Failed())
    case other => throw new RuntimeException("Don't know what to do with message " + other)
  }

  def returnState(requestingNode: String, taskState: TaskState): Unit = {
    log.info("Node " + requestingNode + " asked for our state (it's '" + taskState.getClass.getSimpleName + "')")
    self.reply_?(taskState)
  }

  // TODO: if we allow more than one person to execute the task (speculative execution), do they each get separate Actors?
  // Or does the Actor track the simultaneous executions?
  // Or, perhaps, does a Task actor supervise multiple TaskExecution actors? 

  // we start out in "ready for checkout" mode

  def receive = readyForCheckout

}

case class TaskInfo(taskDefinition: TaskDefinition, properties: Map[String, String]);

/**
 * Our protocol for tasks 
 * I'm not sure that "requestingNode" is necessary in any of these cases, there's no actual "authorization" logic in place.  
 * We do track the node doing the checkout, though.
 */

case class Checkout(val requestingNode: String)

/**
 *  Add a child to this task
 */

case class AddChild(val requestingNode: String, val taskDefinition: TaskDefinition)

/**
 *  Get this task's children
 */

case class GetChildren(val requestingNode: String)

/**
 *  Get information about this task
 */

case class GetTaskInfo(val requestingNode: String)


/**
 * Keep the task checked out, but update its properties 
 */

case class Update(val requestingNode: String, val properties: Map[String, String])

/**
 *  Check this task in.  Its next state depends on the ExecutionStatus
 */

case class Checkin(val requestingNode: String, val executionStatus: ExecutionStatus, val properites: Map[String, String] = Map())

/**
 *  Get the current state of this task
 */

case class GetState(val requestingNode: String)

/**
 *  The current state of the task
 */

sealed abstract class TaskState()

case class Ready() extends TaskState

case class CheckedOut(node: String) extends TaskState

// these two will eventually have more detail in them -- when they were executed and by whom, for example.

case class Failed() extends TaskState

case class Successful() extends TaskState

/**
 *  This could be merged with TaskState, above, and the value named "nextTaskState". 
 */

sealed abstract class ExecutionStatus()

// successful: move to complete (a terminal state)

case class ExecutionSuccessful() extends ExecutionStatus

// failed: move to failed (a terminal state)

case class ExecutionFailed() extends ExecutionStatus

// I'm being lazy in naming here..."Aborted" isn't really the concept I'm going for, but I am trying to communicate
// "I don't want the task to move to a terminal state, but I'm done with it.  Move it back to 'ready'" 
// This status may be removed entirely in the future; it has a nebulous use case.

case class ExecutionAborted() extends ExecutionStatus

/*
 * The other way to accomplish this would be to store tasks in a persistent map, with one actor serving as gatekeeper to the map.
 *
 * Some notes on persistent datastructures:
 * (copied from Akka docs) 
 * Akka's STM should only be used with immutable data. This can be costly if you have large datastructures and are using a naive copy-on-write. In order to make working with immutable datastructures fast enough Scala provides what are called Persistent Datastructures. There are currently two different ones:
 *
 *     * HashMap (scaladoc)
 *     * Vector (scaladoc)
 *
 *
 * They are immutable and each update creates a completely new version but they are using clever structural sharing in order to make them almost as fast, for both read and update, as regular mutable datastructures.
 */

