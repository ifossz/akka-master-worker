package org.ifossz.amw.actor

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Try

object TaskExecutor {

  final case class Run[T, R](task: Task[T, R], work: Work[T])

  def props: Props = Props(new TaskExecutor)
}

class TaskExecutor extends Actor with ActorLogging {

  import TaskExecutor._

  override def receive: Receive = {
    case Run(task, work) =>
      val result = Try(task.execution(work.input))
      sender() ! Worker.FinishWork(result)
  }
}
