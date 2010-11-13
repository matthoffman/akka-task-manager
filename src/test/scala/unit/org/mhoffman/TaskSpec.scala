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
 * 
 */
class TaskSpec extends Spec with ShouldMatchers with BeforeAndAfterAll with Logging with TaskDefinitionCreator {
  
  describe("A Task") {
    describe ("(when it is ready to execute)") {
      it ("should be able to be started") {
        // this is my "canary" test -- a simple "make sure Akka is set up properly and I haven't done anything stupid" test.
        val actorRef = actorOf(new Task(createTaskDefinition))
        actorRef.start
        // ok, that was fun.
        actorRef.stop
      }
      
      it ("should not be checked out") {
        
      }

    }
  }
}
