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

class TaskSpec extends Spec with ShouldMatchers with BeforeAndAfterEach with Logging with TaskDefinitionCreator {

  /**
   * Make sure to clean up after ourselves.
   */

  override def afterEach() {
    ActorRegistry.shutdownAll
  }

  describe("A Task") {

    describe("when it is first started ") {
      it("should start successfully") {
        // this is my "canary" test -- a simple "make sure Akka is set up properly and I haven't done anything stupid" test.
        val actorRef = actorOf(new Task(createTaskDefinition))
        actorRef.start
        // ok, that was fun.
        actorRef.stop
      }

      it("should not be checked out") {
        val actorRef = actorOf(new Task(createTaskDefinition))
        actorRef.start
        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        log.info("Result: " + result)
        result should equal(Ready())
        actorRef.stop
      }

      it("should be able to be checked out") {
        val actorRef = actorOf(new Task(createTaskDefinition))
        actorRef.start
        actorRef ! Checkout("myNode")

        val result = actorRef !! GetState("myNode")
        result should equal(Some(CheckedOut("myNode")))
        actorRef.stop
      }

      it("should start up any children defined in the TaskDefinition") {
        val actorRef = actorOf(new Task(TaskDefinition(name = "Test task", worker = {
          taskContext => log.info("test task executing")
        }, properties = Map("somekey" -> "somevalue"), children = List(
          TaskDefinition(name = "Test task", worker = {
            taskContext => log.info("child 1 executing")
          }, properties = Map("key 1" -> "somevalue 1")),
          TaskDefinition(name = "Test task", worker = {
            taskContext => log.info("child 2 executing")
          }, properties = Map("key 2" -> "somevalue 2")),
          TaskDefinition(name = "Test task", worker = {
            taskContext => log.info("child 3 executing")
          }, properties = Map("key 3" -> "somevalue 3"))
        ))))
        actorRef.start

        val taskInfo = (actorRef !! GetTaskInfo("myNode")).getOrElse(fail())
        log.info("TaskInfo: " + taskInfo);

        //TODO: verify what we want to do.
        fail("Not yet implemented")
        actorRef.stop
      }
    }

    describe("once checked out") {
      it("should not be able to be checked out again") {
        val actorRef = checkoutTask
        actorRef ! Checkout("myNode")

        // This should throw an exception, but I don't know how to tell it that
        // Ah, but the problem now is that it's throwing the exception, but I'm not catching it here.... 
        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        result should equal(CheckedOut("myNode"))
        actorRef.stop
      }

      it("should be able to be checked back in again") {
        val actorRef = checkoutTask
        actorRef ! Checkin("myNode", ExecutionSuccessful())

        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        result should equal(Successful)
        actorRef.stop
      }

      it("should be able to be checked back in again, failing") {
        val actorRef = checkoutTask
        actorRef ! Checkin("myNode", ExecutionFailed())

        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        result should equal(Failed)
        actorRef.stop
      }

      it("should be able to be checked back in again, going back to READY") {
        val actorRef = checkoutTask
        actorRef ! Checkin("myNode", ExecutionAborted())

        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        result should equal(Ready())
        actorRef.stop
      }

      it("should be able to be updated") {
        val actorRef = checkoutTask
        actorRef ! Update("myNode", Map("test" -> "value", "another" -> "boring test value"))

        val taskInfo = (actorRef !! GetTaskInfo("myNode")).getOrElse(fail())
        taskInfo.asInstanceOf[TaskInfo].properties should equal(Map("test" -> "value", "another" -> "boring test value"))

        val result = (actorRef !! GetState("myNode")).getOrElse(fail())
        result should equal(CheckedOut("myNode"))
        actorRef.stop
      }

    }

    describe("should allow children") {
      it("when checked out") {
        val actorRef = checkoutTask
        actorRef ! AddChild("myNode", TaskDefinition(name = "child", worker = {
          taskContext => log.info("child task executing")
        }))


        actorRef.stop
      }
    }
  }

  def checkoutTask(): ActorRef = {
    val actorRef = actorOf(new Task(createTaskDefinition))
    actorRef.start
    actorRef ! Checkout("myNode")

    val result = actorRef !! GetState("myNode")
    result should equal(Some(CheckedOut("myNode")))
    actorRef
  }
}
