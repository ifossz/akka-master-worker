package org.ifossz.amw.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.ifossz.amw.actor.WorkerSpec
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, GivenWhenThen, Matchers}

abstract class ActorSpec extends TestKit(ActorSystem(classOf[WorkerSpec].getSimpleName))
  with ImplicitSender
  with FunSpecLike
  with Matchers
  with BeforeAndAfterAll
  with GivenWhenThen {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
