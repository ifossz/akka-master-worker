package org.ifossz.amw.actor

import akka.actor.{Actor, ActorRef, Props}

object Master {

  final case class Register(worker: ActorRef)

  final case object NextTask

  final case class TaskSucceed(taskId: String, result: Any)

  final case class TaskRejected(taskId: String, reason: String)

  final case class TaskFailed(taskId: String, reason: String)

  def props: Props = Props(new Master)
}

class Master extends Actor {
  override def receive: Receive = ???
}
