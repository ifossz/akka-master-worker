package org.ifossz.amw.actor

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}

object Master {

  final case class RegisterWorker(worker: ActorRef)

  final case object GetWorkers

  final case class RegisteredWorkers(workers: Set[ActorRef])

  final case object NextWork

  final case object NextTask

  final case class WorkSucceed(taskId: TaskId, workId: WorkId, result: Any)

  final case class WorkFailed(taskId: TaskId, workId: WorkId, reason: String)

  def props(nrOfWorkers: Int): Props = Props(new Master(nrOfWorkers))
}

class Master(nrOfWorkers: Int) extends Actor {

  import context._

  private var workers = Set.empty[ActorRef]
  private var router = {
    val routees = Vector.fill(nrOfWorkers) {
      val worker = actorOf(Worker.props)
      watch(worker)
      ActorRefRoutee(worker)
    }
    Router(BroadcastRoutingLogic(), routees)
  }

  router.route(Worker.Enlist(self), self)

  override def receive: Receive = {
    case Master.GetWorkers =>
      sender() ! Master.RegisteredWorkers(workers)

    case Master.RegisterWorker(worker) =>
      workers += worker

    case Terminated(worker) =>
      removeWorker(worker)
      addWorker()
  }

  private def removeWorker(worker: ActorRef): Unit = {
    router = router.removeRoutee(worker)
    workers -= worker
  }

  private def addWorker(): Unit = {
    val worker = actorOf(Worker.props)
    watch(worker)
    router = router.addRoutee(worker)
    workers += worker
  }

}
