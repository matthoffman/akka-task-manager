package unit.org.mhoffman

import org.scalatest.matchers.ShouldMatchers
import se.scalablesolutions.akka.remote.{RemoteServer, RemoteClient}
import tools.NetworkProxy
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
 * Some of these tests are going to be unreasonably pedantic -- essentially testing to make sure language features work.
 *
 * But it's just my way of making sure I understand them properly.
 *
 */
class TaskSpec extends Spec with ShouldMatchers with BeforeAndAfterAll with Logging {

  def createTaskDefinition: TaskDefinition = {
    TaskDefinition(name = "new Task", worker = "some worker")
  }
  describe("A Task Definition") {
    it("should be able to be instantiated") {
      createTaskDefinition
    }
    it("should be able to have children") {
      val parent : TaskDefinition = TaskDefinition(
        name = "new Task", 
        worker = "some worker", 
        children = List(
          TaskDefinition(name = "child 1", worker = "some worker"),
          TaskDefinition(name = "child 2", worker = "some worker")
          )
        )
      println("Parent: "+ parent)
      parent.children should have size 2
      parent.children(0).name should equal ("child 1")
      parent.children(0).worker should equal ("some worker")

      parent.children(1).name should equal ("child 2")
      parent.children(1).worker should equal ("some worker")

    }
  }
  
  describe("A Task") {
    describe ("(when it is ready to execute)") {
      it ("should be able to be started") {
        // this is my "canary" test -- really just watching Akka do its thing
        val actorRef = actorOf(new Task(createTaskDefinition))
        actorRef.start
        
      }
      
      it ("should not be checked out") {
        
      }
    }
  }
}
