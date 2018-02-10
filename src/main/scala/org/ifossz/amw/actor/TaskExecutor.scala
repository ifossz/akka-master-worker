package org.ifossz.amw.actor

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Try

object TaskExecutor {

  final case class Task(input: Seq[Any], executable: Seq[Any] => Any)

  def props: Props = Props(new TaskExecutor)
}

class TaskExecutor extends Actor with ActorLogging {

  import TaskExecutor._

  override def receive: Receive = {
    case Task(input, executable) =>
      val result = Try(executable(input))
      sender() ! Worker.TaskExecution(result)
  }
}
