package unit.org.mhoffman

import org.scalatest.matchers.ShouldMatchers
import se.scalablesolutions.akka.remote.{RemoteServer, RemoteClient}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.{ActorRef, Actor}
import java.util.concurrent.{TimeUnit, CyclicBarrier}
import org.scalatest.{BeforeAndAfterAll, Spec}
import se.scalablesolutions.akka.util.Logging
import unit.test.proto.Commands.WorkerCommand
import java.io.{InputStream, OutputStream}
import java.net.Socket
import org.mhoffman.{TaskDefinition, Task}

/**
 * Most of these tests are going to seem unreasonably pedantic... 
 * they're just me making sure I understand how the language features work.
 */

class TaskDefinitionSpec extends Spec with ShouldMatchers with BeforeAndAfterAll with Logging with TaskDefinitionCreator {


  describe("A Task Definition") {
    it("should be able to be instantiated") {
      createTaskDefinition
    }

    it("should be able to have children") {
      val parent: TaskDefinition = TaskDefinition(
        name = "new Task",
        worker = {
          taskContext => log.info("task executing")
        },
        children = List(
          TaskDefinition(name = "child 1", worker = {
            taskContext => log.info("task executing")
          }),
          TaskDefinition(name = "child 2", worker = {
            taskContext => log.info("Task executing")
          })
        )
      )
      println("Parent: " + parent)
      parent.children should have size 2
      parent.children(0).name should equal("child 1")
      //TODO: fix the worker specs
      //      parent.children(0).worker should equal ("some worker")

      parent.children(1).name should equal("child 2")
      //TODO: fix the worker specs
      //      parent.children(1).worker should equal ("some worker")

    }

    it("should have optional properties") {
      val task = TaskDefinition(
        name = "new task",
        worker = {
          taskContext => log.info("task executing")
        },
        children = List(TaskDefinition(name = "child 1", worker = {
          taskContext => log.info("task executing")
        })),
        properties = Map("key" -> "value", "key 2" -> "value 2")
      )

      println("Task: " + task)
      task.children should have size 1
      // look ma! properties!
      task.properties should have size 2
      task.properties should contain key ("key")
      task.properties("key") should equal("value")
      task.properties should contain key ("key 2")
      task.properties("key 2") should equal("value 2")
    }
  }

}

trait TaskDefinitionCreator {
  def createTaskDefinition: TaskDefinition = {
    TaskDefinition(name = "new Task", worker = {
      taskContext => log.info("Task executing")
    })
  }
}
