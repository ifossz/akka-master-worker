package org.ifossz.amw

import java.util.concurrent.TimeUnit

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class SettingsImpl(config: Config) extends Extension {

  object Worker {
    val WaitingTimeout: FiniteDuration = FiniteDuration(
      config.getDuration("amw.worker.waitingTimeout", TimeUnit.MILLISECONDS),
      TimeUnit.MILLISECONDS)
  }

}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def lookup(): ExtensionId[_ <: Extension] = Settings

  override def createExtension(system: ExtendedActorSystem): SettingsImpl =
    new SettingsImpl(system.settings.config)
}