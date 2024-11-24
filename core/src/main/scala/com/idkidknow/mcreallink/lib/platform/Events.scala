package com.idkidknow.mcreallink.lib.platform

import cats.effect.std.Dispatcher

/** Events registering at the init phase */
trait Events[P[_], F[_]] {
  def onServerStarting(dispatcher: Dispatcher[F])(
      action: P[MinecraftServer] => F[Unit]
  ): F[Unit]
  def onServerStopping(dispatcher: Dispatcher[F])(
      action: P[MinecraftServer] => F[Unit]
  ): F[Unit]
  def onCallingStartCommand(dispatcher: Dispatcher[F])(
      action: () => F[Either[Throwable, Unit]]
  ): F[Unit]
  def onCallingStopCommand(dispatcher: Dispatcher[F])(
      action: () => F[Unit]
  ): F[Unit]
  def onBroadcastingMessage(dispatcher: Dispatcher[F])(
      action: P[Component] => F[Unit]
  ): F[Unit]
}

object Events {
  def apply[P[_], F[_]](using inst: Events[P, F]): Events[P, F] = inst

  given platformEvents[P[_], F[_]](using p: Platform[P, F]): Events[P, F] =
    p.events
}
