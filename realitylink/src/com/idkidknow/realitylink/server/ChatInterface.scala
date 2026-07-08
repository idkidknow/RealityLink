package com.idkidknow.realitylink.server

import cats.effect.Async
import cats.effect.std.Queue
import com.idkidknow.realitylink.lib.CallbackBundle
import com.idkidknow.realitylink.platform.Component
import com.idkidknow.realitylink.platform.Language
import com.idkidknow.realitylink.platform.MinecraftServer
import fs2.Stream

trait ChatInterface[F[_]] {
  def outwardMessages: Stream[F, ChatEvent]

  def broadcastInGame(req: BroadcastRequest): F[Unit]
}

object ChatInterface {
  def apply[F[_]: Async](
      server: MinecraftServer,
      broadcastingMessage: CallbackBundle[F, Component, Unit],
      language: String => Option[String],
  ): ChatInterface[F] = {
    val lang = Language(language)
    new ChatInterface[F] {
      override def outwardMessages: Stream[F, ChatEvent] =
        broadcastingMessage.registerAsStream(Queue.dropping(10)).map {
          component =>
            ChatEvent(
              component.serialize(server),
              component.translateWith(lang),
            )
        }

      override def broadcastInGame(req: BroadcastRequest): F[Unit] = {
        val component: Component = req match {
          case BroadcastRequest.Json(json) =>
            Component
              .deserialize(json, server)
              .getOrElse(Component.literal(json))
          case BroadcastRequest.Literal(text) => Component.literal(text)
        }
        Async[F].delay(server.broadcastMessage(component))
      }
    }
  }
}
