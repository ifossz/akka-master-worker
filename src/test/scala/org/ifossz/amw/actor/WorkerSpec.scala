package org.ifossz.amw.actor

import akka.actor.ActorRef
import akka.testkit.TestProbe
import org.ifossz.amw.Settings
import org.ifossz.amw.common.ActorSpec

import scala.concurrent.duration._

class WorkerSpec extends ActorSpec {

  trait MasterWorker {
    val master: TestProbe = new TestProbe(system)
    val worker: ActorRef = system.actorOf(Worker.props)
  }

  private val settings = Settings(system)

  describe("A worker") {
    describe("when idle") {
      def IdleWorker = new MasterWorker {}

      it("should do nothing if message received") {
        val idle = IdleWorker
        idle.worker ! Worker.Task("task-1", Seq.empty, null)
        idle.master.expectNoMessage(3.seconds)
      }

      it("should register upon master request") {
        val idle = IdleWorker
        idle.worker ! Worker.Enlist(idle.master.ref)
        idle.master.expectMsg(Master.Register(idle.worker))
      }
    }

    describe("when waiting for work") {
      def WaitingWorker = new MasterWorker {
        worker ! Worker.Enlist(master.ref)
        master.expectMsg(Master.Register(worker))
      }

      it("should request work from master if it doesn't receive one") {
        val waiting = WaitingWorker
        waiting.master.expectMsg(settings.Worker.WaitingTimeout + 1.second, Master.NextTask)
      }

      it("should execute received tasks") {
        val waiting = WaitingWorker
        waiting.worker ! Worker.Task("task-1", 1 to 10, (xs: Seq[Any]) => xs.asInstanceOf[Seq[Int]].sum)
        waiting.master.expectMsg(Master.TaskSucceed("task-1", 55))
      }

      it("should be retired if master request so") {
        val waiting = WaitingWorker
        waiting.worker ! Worker.Retire
        waiting.master.expectNoMessage(settings.Worker.WaitingTimeout + 1.second)
      }
    }

    describe("when working") {
      def BusyWorker(taskId: String, busyTime: Long, failTask: Boolean = false) = new MasterWorker {
        worker ! Worker.Enlist(master.ref)
        master.expectMsg(Master.Register(worker))
        master.expectMsg(settings.Worker.WaitingTimeout + 1.second, Master.NextTask)

        val N = 10
        worker ! Worker.Task("task-1", 1 to N, (xs: Seq[Any]) => {
          Thread.sleep(busyTime)
          if (failTask) {
            throw new RuntimeException("error")
          } else {
            xs.asInstanceOf[Seq[Int]].sum
          }
        })

        val expectedResult: Int = N * (N + 1) / 2
      }

      it("should not accept more tasks") {
        val busy = BusyWorker("task-1", busyTime = 2000)
        busy.worker ! Worker.Task("task-2", 1 to 10, (_: Seq[Any]) => "should not be executed")
        busy.master.expectMsgClass(classOf[Master.TaskRejected]).taskId shouldBe "task-2"
        busy.master.expectMsg(Master.TaskSucceed("task-1", busy.expectedResult))
      }

      it("should finish the current task and wait for a new one") {
        val busy = BusyWorker("task-1", busyTime = 0)
        busy.master.expectMsg(Master.TaskSucceed("task-1", busy.expectedResult))
        busy.master.expectMsg(settings.Worker.WaitingTimeout + 1.second, Master.NextTask)
      }

      it("should finish the current task and wait for a new one even if the task failed") {
        val busy = BusyWorker("task-1", busyTime = 0, failTask = true)
        busy.master.expectMsgClass(classOf[Master.TaskFailed]).taskId shouldBe "task-1"
        busy.master.expectMsg(settings.Worker.WaitingTimeout + 1.second, Master.NextTask)
      }

      it("should be retired if master requests so after work has been completed") {
        val busy = BusyWorker("task-1", busyTime = 1000)
        busy.worker ! Worker.Retire
        busy.master.expectMsg(Master.TaskSucceed("task-1", busy.expectedResult))
        busy.master.expectNoMessage(settings.Worker.WaitingTimeout + 1.second)
      }
    }
  }
}
