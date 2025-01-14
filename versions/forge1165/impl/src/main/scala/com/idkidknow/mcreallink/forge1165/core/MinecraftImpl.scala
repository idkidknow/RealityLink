package com.idkidknow.mcreallink.forge1165.core

import com.idkidknow.mcreallink.api.Minecraft
import com.idkidknow.mcreallink.forge1165.mixin.complement.{
  BroadcastingMessage,
  ServerTranslate,
}
import net.minecraft.network.chat.{ChatType, FormattedText, TextComponent}
import net.minecraft.util.FormattedCharSequence
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.event.server.{
  FMLServerStartingEvent,
  FMLServerStoppingEvent,
}
import net.minecraftforge.fml.loading.FMLPaths

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  override type Component = net.minecraft.network.chat.Component
  override type Language = net.minecraft.locale.Language
  override type MinecraftServer = net.minecraft.server.MinecraftServer

  override val Component: ComponentClass = new ComponentClass {
    override def translateWith(
        component: Component,
        language: Language,
    ): String =
      ServerTranslate.translate(component, language)

    override def literal(text: String): Component = TextComponent(text)

    override def serialize(
        component: Component,
        server: MinecraftServer,
    ): String =
      net.minecraft.network.chat.Component.Serializer.toJson(component)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[Component] = try {
      Some(net.minecraft.network.chat.Component.Serializer.fromJson(json))
    } catch {
      case _: Exception => None
    }
  }
  override val Language: LanguageClass = new LanguageClass {
    override def make(map: String => Option[String]): Language = new Language {
      override def getOrDefault(id: String): String = map(id).getOrElse(id)

      override def has(id: String): Boolean = map(id).isDefined

      override def isDefaultRightToLeft: Boolean = false

      override def getVisualOrder(text: FormattedText): FormattedCharSequence =
        FormattedCharSequence.EMPTY
    }

    override def parseLanguageFile(
        stream: InputStream
    ): Option[Map[String, String]] = try {
      import scala.collection.mutable
      val map = mutable.HashMap.empty[String, String]
      net.minecraft.locale.Language.loadFromJson(stream, map.put)
      Some(map.toMap)
    } catch {
      case _: Exception => None
    }

    override def languageFileExtension: String = "json"
  }
  override val MinecraftServer: MinecraftServerClass =
    new MinecraftServerClass {
      override def broadcastMessage(
          server: MinecraftServer,
          message: Component,
      ): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getPlayerList.broadcastMessage(
            message,
            ChatType.SYSTEM,
            net.minecraft.Util.NIL_UUID,
          )
        }
    }
  override val events: Events = new Events {
    override def setOnServerStarting(action: MinecraftServer => Unit): Unit =
      MinecraftForge.EVENT_BUS.addListener[FMLServerStartingEvent] { event =>
        action(event.getServer)
      }

    override def setOnServerStopping(action: () => Unit): Unit =
      MinecraftForge.EVENT_BUS.addListener[FMLServerStoppingEvent] { _ =>
        action()
      }

    override def setOnCallingStartCommand(
        action: () => Either[Throwable, Unit]
    ): Unit =
      InitCommands.startAction = action

    override def setOnCallingStopCommand(action: () => Unit): Unit =
      InitCommands.stopAction = action

    override def setOnCallingDownloadCommand(action: () => Unit): Unit =
      InitCommands.downloadAction = action

    override def setOnBroadcastingMessage(action: Component => Unit): Unit =
      BroadcastingMessage.setCallback(component => action(component))
  }

  override def gameRootDirectory: Path = FMLPaths.GAMEDIR.get()

  override def configDirectory: Path = FMLPaths.CONFIGDIR.get()

  override def minecraftVersion: String = "1.16.5"
}
