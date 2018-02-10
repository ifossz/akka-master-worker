package org.ifossz.amw.actor

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Routee, Router}

object Master {

  final case class Register(worker: ActorRef)

  final case object GetWorkers

  final case class RegisteredWorkers(workers: Seq[Routee])

  final case object NextTask

  final case class TaskSucceed(taskId: String, result: Any)

  final case class TaskRejected(taskId: String, reason: String)

  final case class TaskFailed(taskId: String, reason: String)

  def props(nrOfWorkers: Int): Props = Props(new Master(nrOfWorkers))
}

class Master(nrOfWorkers: Int) extends Actor {

  import context._

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
      sender() ! Master.RegisteredWorkers(router.routees)

    case Terminated(actor) =>
      router = router.removeRoutee(actor)
      val worker = actorOf(Worker.props)
      watch(worker)
      router = router.addRoutee(worker)
  }
}
