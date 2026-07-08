package com.idkidknow.realitylink.platform.api

trait Hooks extends Types {

  /** `ServerStartingEvent`, `ServerLifecycleEvents.SERVER_STARTING` */
  def setOnServerStarting(action: MinecraftServer => Unit): Unit

  /** `ServerStoppingEvent`, `ServerLifecycleEvents.SERVER_STOPPING` */
  def setOnServerStopping(action: () => Unit): Unit

  def setOnCallingStartCommand(action: () => Either[Throwable, Unit]): Unit

  def setOnCallingStopCommand(action: () => Unit): Unit

  def setOnCallingDownloadCommand(action: () => Unit): Unit

  def setOnBroadcastingMessage(action: Component => Unit): Unit
}
