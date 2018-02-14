package org.ifossz.amw.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Terminated}
import org.ifossz.amw.Settings

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object Worker {

  final case class Enlist(master: ActorRef)

  final case class AssignTask[T, R](task: Task[T, R])

  final case class StartWork[T](work: Work[T])

  final case class FinishWork[T](result: Try[T])

  final case object Retire

  def props: Props = Props(new Worker)
}

class Worker extends Actor with ActorLogging {

  import Worker._
  import context._

  private val settings = Settings(context.system)

  private def idle: Receive = {
    case Enlist(master) =>
      master ! Master.RegisterWorker(self)
      setReceiveTimeout(settings.Worker.RequestNextTaskInterval)
      become(waitingForTask(master))
  }

  private def waitingForTask(master: ActorRef): Receive = {
    case ReceiveTimeout =>
      master ! Master.NextTask

    case AssignTask(task) =>
      setReceiveTimeout(settings.Worker.RequestNextWorkInterval)
      become(waitingForWork(master, task))

    case Retire =>
      setReceiveTimeout(Duration.Undefined)
      become(idle)
  }

  private def waitingForWork(master: ActorRef, task: Task[Any, Any]): Receive = {
    case ReceiveTimeout =>
      master ! Master.NextWork

    case StartWork(work) =>
      val taskExecutor = actorOf(TaskExecutor.props)
      taskExecutor ! TaskExecutor.Run(task, work)
      setReceiveTimeout(Duration.Undefined)
      become(working(master, task, work, taskExecutor))

    case AssignTask(newTask) =>
      become(waitingForWork(master, newTask))

    case Retire =>
      setReceiveTimeout(Duration.Undefined)
      become(idle)
  }

  private def working(master: ActorRef,
                      task: Task[Any, Any],
                      work: Work[Any],
                      taskExecutor: ActorRef,
                      retireAfterExecution: Boolean = false): Receive = {
    case FinishWork(result) =>
      val message = result match {
        case Success(value) =>
          Master.WorkSucceed(task.id, work.id, value)
        case Failure(ex) =>
          val error = s"Error executing Work[${work.id}] for Task[${task.id}]. Exception type: ${ex.getClass.getName}. Error: ${ex.getMessage}"
          log.error(ex, error)
          Master.WorkFailed(task.id, work.id, error)
      }

      master ! message
      if (retireAfterExecution) {
        become(idle)
      } else {
        setReceiveTimeout(settings.Worker.RequestNextWorkInterval)
        become(waitingForWork(master, task))
      }

    case Retire =>
      become(working(master, task, work, taskExecutor, retireAfterExecution = true))

    case Terminated(`taskExecutor`) =>
      val error = s"Error executing Work[${work.id}] for Task[${task.id}]. Task executor was unexpectedly terminated"
      log.error(error)
      Master.WorkFailed(task.id, work.id, error)
  }

  override def receive: Receive = idle
}
