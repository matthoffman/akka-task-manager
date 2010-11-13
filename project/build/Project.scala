import sbt._
import Process._
import sbt_akka_bivy._

class Project(info: ProjectInfo) extends DefaultProject(info) with AkkaProject with AkkaKernelDeployment {
  val bumRepo = "Bum Networks Release Repository" at "http://repo.bumnetworks.com/releases"
  val bumSnapshots = "Bum Networks Snapshot Repository" at "http://repo.bumnetworks.com/snapshots"

  val akkaCamel = akkaModule("camel")
  val akkaKernel = akkaModule("kernel")
  val akkaRedis = akkaModule("persistence-redis")
  val junit = "junit" % "junit" % "4.8.1" % "test->default"
  val camelFtp = "org.apache.camel" % "camel-ftp" % "2.4.0" % "compile"
  val camelMina = "org.apache.camel" % "camel-mina" % "2.4.0" % "compile"
  val camelJetty = "org.apache.camel" % "camel-jetty" % "2.4.0" % "compile"
  // for coverage
  val emma = "emma" % "emma" % "2.0.5312" % "test->default"

  // I think this is required for scalatest, but i'm putting it in compile anyway
  //lazy val time = "org.scala-tools" % "time" % "2.8.0-0.2-SNAPSHOT" % "compile" from "http://www.google.com/url?sa=D&q=http://repo.bumnetworks.com/snapshots/org/scala-tools/time/2.8.0-0.2-SNAPSHOT/time-2.8.0-0.2-SNAPSHOT.jar&usg=AFQjCNF_4fcR-GJBKINSEXokva3bE0r3EA"
  val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test->default"
  //val scalatest = "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.final-SNAPSHOT" % "test->default"

  override def repositories = Set(
    "scala-tools-snapshots" at "http://scala-tools.org/repo-snapshots/",
    "bum-networks-snaphost" at "http://repo.bumnetworks.com/snapshots"
  )
  // dont include integration and performance tests by default.
  override def includeTest(s: String) = {!s.contains("integration.") && !s.contains("performance.")}

  lazy val integrationTest =
  defaultTestTask(TestListeners(testListeners) ::
          TestFilter(s => s.contains("integration.")) ::
          Nil)

  lazy val performanceTest =
  defaultTestTask(TestListeners(testListeners) ::
          TestFilter(s => s.contains("performance.")) ::
          Nil)

}
