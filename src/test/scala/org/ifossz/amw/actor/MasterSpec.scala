package org.ifossz.amw.actor

import akka.actor.{ActorRef, PoisonPill}
import akka.testkit._
import org.ifossz.amw.Settings
import org.ifossz.amw.common.ActorSpec

import scala.concurrent.duration._

class MasterSpec extends ActorSpec {

  private val settings = Settings(system)

  describe("A Master") {

    it("should create and enlist workers for distributing tasks") {
      val master = system.actorOf(Master.props(nrOfWorkers = 10))
      awaitAssert {
        master ! Master.GetWorkers
        expectMsgClass(classOf[Master.RegisteredWorkers]).workers should have size 10
      }
    }

    it("should create and enlist a new worker if one is lost") {
      val master = system.actorOf(Master.props(nrOfWorkers = 1))
      val worker = getSingleWorker(master)

      val probe = new TestProbe(system)
      probe.watch(worker)

      worker ! PoisonPill
      probe.expectTerminated(worker)

      val newWorker = getSingleWorker(master)

      worker shouldNot be(newWorker)
    }

    it("should enlist a worker if it gets retired") {
      val master = system.actorOf(Master.props(nrOfWorkers = 1))
      master ! Master.GetWorkers
      val worker = getSingleWorker(master)
      worker ! Worker.Retire
      val reintegratedWorker = getSingleWorker(master, settings.Master.EnlistInterval.dilated)
      worker shouldBe reintegratedWorker
    }
  }

  describe("A Master with a single Worker") {
    val master = system.actorOf(Master.props(nrOfWorkers = 1))

    it("should distribute tasks to worker until source is exhausted") {

    }
  }

  private def getSingleWorker(master: ActorRef, timeOut: FiniteDuration = 3.seconds) = awaitAssert {
    master ! Master.GetWorkers
    val workers = expectMsgClass(timeOut, classOf[Master.RegisteredWorkers]).workers
    workers should have size 1
    workers.head
  }

}
