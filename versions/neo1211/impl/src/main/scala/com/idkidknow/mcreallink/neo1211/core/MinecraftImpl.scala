package com.idkidknow.mcreallink.neo1211.core

import com.idkidknow.mcreallink.api.Minecraft
import com.idkidknow.mcreallink.neo1211.mixin.complement.{
  BroadcastingMessage,
  ServerTranslate,
}
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.{
  ServerStartingEvent,
  ServerStoppingEvent,
}

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  override type Component = net.minecraft.network.chat.Component
  override type Language = net.minecraft.locale.Language
  override type MinecraftServer = net.minecraft.server.MinecraftServer

  override val Component: ComponentClass = new ComponentClass {
    override def translateWith(component: Component, language: Language): String =
      ServerTranslate.translate(component, language)

    override def literal(text: String): Component = net.minecraft.network.chat.Component.literal(text)

    override def serialize(component: Component, server: MinecraftServer): String =
      net.minecraft.network.chat.Component.Serializer.toJson(component, server.registryAccess())

    override def deserialize(json: String, server: MinecraftServer): Option[Component] = try {
      Some(net.minecraft.network.chat.Component.Serializer.fromJson(json, server.registryAccess()))
    } catch {
      case _: Exception => None
    }
  }

  override val Language: LanguageClass = new LanguageClass {
    override def make(map: String => Option[String]): Language = new Language {
      private val fallback: Language = net.minecraft.locale.Language.getInstance()
      override def getOrDefault(s: String, defaultValue: String): String =
        map(s).getOrElse(fallback.getOrDefault(s, defaultValue))
      override def has(s: String): Boolean = map(s).nonEmpty
      override def isDefaultRightToLeft: Boolean = false
      override def getVisualOrder(formattedText: FormattedText): FormattedCharSequence =
        FormattedCharSequence.EMPTY
    }

    override def parseLanguageFile(stream: InputStream): Option[Map[String, String]] = {
      val map = scala.collection.mutable.HashMap.empty[String, String]
      try {
        net.minecraft.locale.Language.loadFromJson(stream, { (k, v) => map.put(k, v) })
        Some(map.toMap)
      } catch {
        case _: Exception => None
      }
    }

    override def languageFileExtension: String = "json"
  }

  override val MinecraftServer: MinecraftServerClass = new MinecraftServerClass {
    override def broadcastMessage(server: MinecraftServer, message: Component): Unit = {
      BroadcastingMessage.ignoreTemporarily { () =>
        server.getPlayerList.broadcastSystemMessage(message, false)
      }
    }
  }

  override val events: Events = new Events {
    override def setOnServerStarting(action: MinecraftServer => Unit): Unit = {
      NeoForge.EVENT_BUS.addListener[ServerStartingEvent] { event =>
        action(event.getServer)
      }
    }
    override def setOnServerStopping(action: () => Unit): Unit = {
      NeoForge.EVENT_BUS.addListener[ServerStoppingEvent] { _ =>
        action()
      }
    }
    override def setOnCallingStartCommand(action: () => Either[Throwable, Unit]): Unit =
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
  override def minecraftVersion: String = "1.21.1"
}
