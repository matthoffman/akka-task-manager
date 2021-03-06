package unit.org.mhoffman

import org.scalatest.matchers.ShouldMatchers
import se.scalablesolutions.akka.actor.Actor._
import org.scalatest.{BeforeAndAfterEach, Spec}
import se.scalablesolutions.akka.util.Logging
import org.mhoffman._
import se.scalablesolutions.akka.actor.{ActorRegistry, ActorRef}

/**
 *
 */

class TaskGraphSpec extends Spec with ShouldMatchers with BeforeAndAfterEach with Logging with TaskDefinitionCreator {

  /**
   * These definitions for testGraph, testgraph2, and testgraph3 were originally written as experiments in the simplest, cleanest ways to
   * define a task hierarchy for test purposes.
   * My assumptions for this test hierarchy are:
   * 1.) some tasks will have children, some won't
   * 2.) tasks should be able to specify a "delay" value -- that is the # of milliseconds that the task will pause before being considered complete.
   *  This is to represent the time spent working on that task.
   * 3.) some tasks will throw an exception.  We can define what exception they throw upfront.
   *
   *
   * Below we have two ways of specifying a task hierarchy.  The first uses a case class called "T", which takes optional
   * named parameters for delay, children, and an exception to throw.  The second uses several different case classes,
   * one for a "delay" task ("D"), one for a "failing" task ("F"), and one for a "Normal" task ("N") -- a task without a delay.
   */


  val testGraph = T(delay = 10, children = List(
    T(delay = 100),
    T(
      children = List(
        T(100),
        T(100),
        T(100, throws = new IllegalArgumentException("Expected exception")))
    ),
    T(throws = new IllegalArgumentException("Expected exception"))
  )
  )

  // experimenting with different ways to specify.  I think this looks a little cleaner.
  // D = a task with a delay.  Delay specified in milliseconds.
  // F = a task which will fail (the exception it will throw is passed as an argument
  // N = a "normal" task (no delay, no failures)
  // any of these tasks can take optional children.
  val testGraph2 =
    D(10,
      D(100),
      N(
        D(100),
        D(100),
        F(new IllegalArgumentException("Expected exception"))
      )
    )

  val testGraph3 =
    D(10,
      N(
        /* really just experimenting here with how to say " one of these, then 100 of these, then one of these" */
        List(D(500)) :::
                (for (i <- (0 to 100).toList) yield D(100)) :::
                List(F(new IllegalArgumentException("Expected exception"))): _*
      ),
      F(new IllegalArgumentException("Expected exception"))
    )


  /**
   * Our base class for test tasks.
   */

  abstract class TestTask(childTasks: TestTask*) {

    /**
     * Will be called before children are created
     */

    def beforeChildren(): Unit = {};

    /**
     * Will be called after children are created
     */

    def afterChildren(): Unit = {};

    def toTaskDefinition(): TaskDefinition = {
      new TaskDefinition(name = "test task", worker = {
        context =>
          beforeChildren()

          for (child <- childTasks) {
            context.addChild(child.toTaskDefinition)
          }

          afterChildren()
      })
    }

  }

  /**
   * A delay task.  Delays for the number of milliseconds given in the constructor.
   * This is a case class just to avoid needing "new". Looks a little prettier.
   */

  case class D(delay: Int, childTasks: TestTask*) extends TestTask {

  }

  /**
   * A completely do-nothing task. Equivalent to D(0)
   * Called N (for "normal") because it doesn't do anything special -- no delay, no exceptions, etc.
   *
   */

  case class N(childTasks: TestTask*) extends TestTask

  case class F(throws: Throwable, childTasks: TestTask*) extends TestTask


  /**
   * This is a case class just to avoid needing "new". Looks a little prettier.
   * They tell me you can't use varargs and default parameters, so I'm using a list here instead. Varargs would be nicer, though.
   *
   */

  object T {
    // splitting into two lists of parameters doesn't seem to work as advertised.

    def apply(delay: Int = 0, throws: Throwable = null, children: List[TestTask] = List()): TestTask = {
      D(delay, children: _*)
    }
  }


  var taskGraphRef: ActorRef = null;

  /**
   * Make sure to clean up after ourselves.
   */

  override def beforeEach() {
    taskGraphRef = actorOf(new TaskGraph)
    taskGraphRef.start
  }


  /**
   * Make sure to clean up after ourselves.
   */

  override def afterEach() {
    ActorRegistry.shutdownAll
  }

  describe("taskGraph1") {
    it("should load") {
      taskGraphRef ! testGraph.toTaskDefinition
    }
  }

  describe("taskGraph2") {
    it("should load") {
      taskGraphRef ! testGraph2.toTaskDefinition
    }
  }

  describe("taskGraph3") {
    it("should load") {
      taskGraphRef ! testGraph3.toTaskDefinition
    }
  }
}
