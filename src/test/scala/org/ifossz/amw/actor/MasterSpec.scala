package org.ifossz.amw.actor

import org.ifossz.amw.common.ActorSpec

class MasterSpec extends ActorSpec {

  describe("A Master") {
    it("should create workers for distributing tasks") {
      val master = system.actorOf(Master.props(nrOfWorkers = 10))
      master ! Master.GetWorkers
      expectMsgClass(classOf[Master.RegisteredWorkers]).workers.size shouldBe 10
    }
  }
}
