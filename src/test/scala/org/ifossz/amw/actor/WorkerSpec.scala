package org.ifossz.amw.actor

import akka.actor.ActorRef
import akka.testkit.{TestProbe, _}
import org.ifossz.amw.Settings
import org.ifossz.amw.common.ActorSpec

import scala.concurrent.duration._

class WorkerSpec extends ActorSpec {

  private val settings = Settings(system)

  private trait TestContext {
    val master: TestProbe
    val worker: ActorRef
  }

  private class Idle extends TestContext {
    override lazy val master: TestProbe = new TestProbe(system)
    override lazy val worker: ActorRef = system.actorOf(Worker.props)
  }

  private class WaitingForTask extends Idle {
    worker ! Worker.Enlist(master.ref)
    master.expectMsg(Master.RegisterWorker(worker))
  }

  private class WaitingForWork[-T, +R](task: Task[T, R]) extends WaitingForTask {
    worker ! Worker.AssignTask(task)
  }

  private class Busy[-T, +R](task: Task[T, R], work: Work[T]) extends WaitingForWork(task) {
    worker ! Worker.StartWork(work)
  }

  private def createSumTask(taskId: String,
                            sleep: Option[FiniteDuration] = None,
                            failTask: Boolean = false): Task[Int, Int] = {
    Task(TaskId(taskId), (xs: Seq[Int]) => {
      if (sleep.isDefined) Thread.sleep(sleep.get.toMillis)
      if (failTask) throw new RuntimeException("runtime-error")
      else xs.sum
    })
  }

  describe("A worker") {
    describe("when idle") {
      it("should register upon master request") {
        val idle = new Idle
        idle.worker ! Worker.Enlist(idle.master.ref)
        idle.master.expectMsg(Master.RegisterWorker(idle.worker))
      }
    }

    describe("when waiting for tasks") {
      it("should request tasks from master if it doesn't receive one") {
        val waiting = new WaitingForTask
        waiting.master.expectMsg(settings.Worker.RequestNextTaskInterval.dilated, Master.NextTask)
      }

      it("should wait for work if task is assigned") {
        val waiting = new WaitingForTask
        waiting.worker ! Worker.AssignTask(createSumTask("task-1"))
        waiting.master.expectMsg(settings.Worker.RequestNextWorkInterval.dilated, Master.NextWork)
      }

      it("should be retired if master request so") {
        val waiting = new WaitingForTask
        waiting.worker ! Worker.Retire
        waiting.master.expectNoMessage(settings.Worker.RequestNextTaskInterval.dilated)
      }
    }

    describe("when waiting for work") {
      it("should request work from master if it doesn't receive one") {
        val waiting = new WaitingForWork(createSumTask("task-1"))
        waiting.master.expectMsg(settings.Worker.RequestNextWorkInterval.dilated, Master.NextWork)
      }

      it("should execute received tasks") {
        val waiting = new WaitingForWork(createSumTask("task-1"))
        waiting.worker ! Worker.StartWork(Work(WorkId("work-1"), 1 to 10))
        waiting.master.expectMsg(Master.WorkSucceed(TaskId("task-1"), WorkId("work-1"), 55))
      }

      it("should assign new tasks if master request so") {
        val waiting = new WaitingForWork(createSumTask("task-1"))
        waiting.worker ! Worker.AssignTask(createSumTask("task-2"))
        waiting.worker ! Worker.StartWork(Work(WorkId("work-1"), 1 to 10))
        waiting.master.expectMsg(Master.WorkSucceed(TaskId("task-2"), WorkId("work-1"), 55))
      }

      it("should be retired if master request so") {
        val waiting = new WaitingForWork(createSumTask("task-1"))
        waiting.worker ! Worker.Retire
        waiting.master.expectNoMessage(settings.Worker.RequestNextWorkInterval.dilated)
      }
    }

    describe("when working") {
      it("should not accept more tasks") {
        val busy = new Busy(createSumTask("task-1", sleep = Some(2.second)), Work(WorkId("work-1"), 1 to 10))
        busy.worker ! Worker.StartWork(Work(WorkId("work-1"), 1 to 20))
        busy.master.expectMsg(Master.WorkSucceed(TaskId("task-1"), WorkId("work-1"), 55))
      }

      it("should finish the current task and wait for a new one") {
        val busy = new Busy(createSumTask("task-1"), Work(WorkId("work-1"), 1 to 10))
        busy.master.expectMsg(Master.WorkSucceed(TaskId("task-1"), WorkId("work-1"), 55))
        busy.master.expectMsg(settings.Worker.RequestNextWorkInterval.dilated, Master.NextWork)
      }

      it("should finish the current task and wait for a new one even if the task failed") {
        val busy = new Busy(createSumTask("task-1", failTask = true), Work(WorkId("work-1"), 1 to 10))
        busy.master.expectMsgClass(classOf[Master.WorkFailed]).taskId shouldBe TaskId("task-1")
        busy.master.expectMsg(settings.Worker.RequestNextWorkInterval.dilated, Master.NextWork)
      }

      it("should be retired if master requests so after work has been completed") {
        val busy = new Busy(createSumTask("task-1", sleep = Some(1.second)), Work(WorkId("work-1"), 1 to 10))
        busy.worker ! Worker.Retire
        busy.master.expectMsg(Master.WorkSucceed(TaskId("task-1"), WorkId("work-1"), 55))
        busy.master.expectNoMessage(settings.Worker.RequestNextTaskInterval.dilated)
      }
    }
  }
}
