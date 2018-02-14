package org.ifossz.amw.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.ifossz.amw.actor.WorkerSpec
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, GivenWhenThen, Matchers}

abstract class ActorSpec
  extends TestKit(ActorSystem(classOf[WorkerSpec].getSimpleName, ConfigFactory.parseString(ActorSpec.config)))
    with ImplicitSender
    with FunSpecLike
    with Matchers
    with BeforeAndAfterAll
    with GivenWhenThen {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}

object ActorSpec {
  val config: String =
    """
      |akka {
      |  test {
      |    timefactor = 1.1
      |  }
      |}
      |amw {
      |  worker {
      |    requestNextTaskInterval = 2s
      |    requestNextWorkInterval = 2s
      |  }
      |  master {
      |    enlistInterval = 2s
      |  }
      |}
    """.stripMargin
}
