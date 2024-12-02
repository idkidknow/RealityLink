package com.idkidknow.mcreallink.server

import fs2.Stream

trait MinecraftInterface[F[_]] {
  def outwardMessages: Stream[F, ChatEvent]

  def broadcastInGame(req: BroadcastRequest): F[Unit]
}
