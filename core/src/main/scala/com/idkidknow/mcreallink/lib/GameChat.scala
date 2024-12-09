package com.idkidknow.mcreallink.lib

import cats.effect.kernel.Async
import cats.effect.std.Queue
import com.idkidknow.mcreallink.minecraft.Minecraft
import com.idkidknow.mcreallink.utils.CallbackBundle
import fs2.Stream

trait GameChat[F[_]] {
  type Component
  extension (component: Component) {
    def serialize: String
    def translateWith(language: String => Option[String]): String
    def broadcastInGame: F[Unit]
  }
  def deserializeToComponent(json: String): Option[Component]
  def literalComponent(text: String): Component

  def chatStream: Stream[F, Component]
}

object GameChat {
  def apply[F[_]: Async](using mc: Minecraft)(
      server: mc.MinecraftServer,
      broadcastingMessage: CallbackBundle[F, mc.Component, Unit],
  ): GameChat[F] = new GameChat[F] {
    override type Component = mc.Component

    extension (component: Component) {
      override def serialize: String =
        mc.Component.serialize(component, server)
      override def translateWith(language: String => Option[String]): String =
        mc.Component.translateWith(
          component,
          mc.Language.make(language),
        )
      override def broadcastInGame: F[Unit] = Async[F].delay {
        mc.MinecraftServer.broadcastMessage(server, component)
      }
    }

    override def deserializeToComponent(json: String): Option[Component] =
      mc.Component.deserialize(json, server)

    override def literalComponent(text: String): Component =
      mc.Component.literal(text)

    override def chatStream: Stream[F, Component] =
      broadcastingMessage.registerAsStream(Queue.dropping(10))

  }
}
