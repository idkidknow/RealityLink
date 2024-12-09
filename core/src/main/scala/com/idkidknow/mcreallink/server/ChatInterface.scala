package com.idkidknow.mcreallink.server

import fs2.Stream
import com.idkidknow.mcreallink.lib.GameChat

trait ChatInterface[F[_]] {
  def outwardMessages: Stream[F, ChatEvent]

  def broadcastInGame(req: BroadcastRequest): F[Unit]
}

object ChatInterface {
  def apply[F[_]](
      gameChat: GameChat[F],
      language: String => Option[String],
  ): ChatInterface[F] = new ChatInterface[F] {
    import gameChat.Component
    override def outwardMessages: Stream[F, ChatEvent] =
      gameChat.chatStream.map { component =>
        ChatEvent(component.serialize, component.translateWith(language))
      }
    override def broadcastInGame(req: BroadcastRequest): F[Unit] = {
      val component: Component = req match {
        case BroadcastRequest.Json(json) =>
          gameChat
            .deserializeToComponent(json)
            .getOrElse(gameChat.literalComponent(json))
        case BroadcastRequest.Literal(text) => gameChat.literalComponent(text)
      }
      component.broadcastInGame
    }
  }
}
