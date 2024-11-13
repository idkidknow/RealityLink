package com.idkidknow.mcreallink.lib.platform

trait Events[P[_], F[_]] {
  def onServerStarting(action: P[MinecraftServer] => F[Unit]): F[Unit]
  def onServerStopping(action: P[MinecraftServer] => F[Unit]): F[Unit]
  def onCallingStartCommand(action: () => F[Either[Throwable, Unit]]): F[Unit]
  def onCallingStopCommand(action: () => F[Unit]): F[Unit]
  def onBroadcastingMessage(action: P[Component] => F[Unit]): F[Unit]
}

object Events {
  def apply[P[_], F[_]](using inst: Events[P, F]) = inst

  given platformEvents[P[_], F[_]](using p: Platform[P, F]): Events[P, F] =
    p.events
}
