package org.ifossz.amw.actor

import org.ifossz.amw.common.ActorSpec

import scala.util.Success

class TaskExecutorSpec extends ActorSpec {

  describe("A TaskExecutor") {
    it("should execute a task") {
      val taskExecutor = system.actorOf(TaskExecutor.props)
      taskExecutor ! TaskExecutor.Run(Task(TaskId("task-1"), (xs: Seq[Int]) => xs.sum), Work(WorkId("work-1"), 1 to 10))
      expectMsg(Worker.FinishWork(Success(55)))
    }

    it("should return a result even if the task failed") {
      val taskExecutor = system.actorOf(TaskExecutor.props)
      taskExecutor ! TaskExecutor.Run(Task(TaskId("task-1"), (xs: Seq[Int]) => (throw new RuntimeException("runtime-error")): Int), Work(WorkId("work-1"), 1 to 10))
      expectMsgClass(classOf[Worker.FinishWork[Int]]).result.isSuccess shouldBe false
    }
  }
}
