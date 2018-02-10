package org.ifossz.amw.actor

import org.ifossz.amw.common.ActorSpec

import scala.util.{Failure, Success}

class TaskExecutorSpec extends ActorSpec {

  describe("A TaskExecutor") {
    it("should execute a task") {
      val taskExecutor = system.actorOf(TaskExecutor.props)
      taskExecutor ! TaskExecutor.Task(1 to 10, (xs: Seq[Any]) => xs.asInstanceOf[Seq[Int]].sum)
      expectMsg(Worker.TaskExecution(Success(55)))
    }

    it("should return a result even if the task failed") {
      val taskExecutor = system.actorOf(TaskExecutor.props)
      taskExecutor ! TaskExecutor.Task(1 to 10, (_: Seq[Any]) => throw new RuntimeException("error"))
      expectMsgClass(classOf[Worker.TaskExecution]).result.isSuccess shouldBe false
    }
  }
}
