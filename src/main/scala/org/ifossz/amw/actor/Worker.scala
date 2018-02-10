package org.ifossz.amw.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import org.ifossz.amw.Settings

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object Worker {

  final case class Enlist(master: ActorRef)

  final case class Task(taskId: String, input: Seq[Any], executable: Seq[Any] => Any)

  final case class TaskExecution(result: Try[Any])

  final case object Retire

  def props: Props = Props(new Worker)
}

class Worker extends Actor with ActorLogging {

  import Worker._
  import context._

  private val settings = Settings(context.system)
  private val taskExecutor = context.actorOf(TaskExecutor.props)

  def idle: Receive = {
    case Enlist(master) =>
      master ! Master.Register(self)
      setReceiveTimeout(settings.Worker.WaitingTimeout)
      become(waitingForWork(master))
  }

  def waitingForWork(master: ActorRef): Receive = {
    case ReceiveTimeout =>
      master ! Master.NextTask

    case Task(taskId, input, executable) =>
      taskExecutor ! TaskExecutor.Task(input, executable)
      setReceiveTimeout(Duration.Undefined)
      become(working(master, taskId))

    case Retire =>
      setReceiveTimeout(Duration.Undefined)
      become(idle)
  }

  def working(master: ActorRef, taskId: String, retireAfterExecution: Boolean = false): Receive = {
    case Task(rejectedTaskId, _, _) =>
      val error = s"Task[$rejectedTaskId] rejected because the Worker is busy processing Task[$taskId]"
      master ! Master.TaskRejected(rejectedTaskId, error)

    case TaskExecution(result) =>
      val message = result match {
        case Success(value) =>
          Master.TaskSucceed(taskId, value)
        case Failure(ex) =>
          val error = s"Error executing Task[$taskId]. Exception type: ${ex.getClass.getName}. Error: ${ex.getMessage}"
          log.error(ex, error)
          Master.TaskFailed(taskId, error)
      }

      master ! message
      if (retireAfterExecution) {
        become(idle)
      } else {
        setReceiveTimeout(settings.Worker.WaitingTimeout)
        become(waitingForWork(master))
      }

    case Retire =>
      become(working(master, taskId, retireAfterExecution = true))
  }

  override def receive: Receive = idle
}
