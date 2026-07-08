package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.neo1211.mixin.BroadcastingMessage
import com.idkidknow.realitylink.neo1211.mixin.ServerTranslate
import com.idkidknow.realitylink.platform.Platform.Component
import com.idkidknow.realitylink.platform.Platform.MinecraftServer
import com.idkidknow.realitylink.platform.api.API
import fs2.io.file.Path
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent

import scala.util.Try

object Platform extends API {
  opaque type Component = net.minecraft.network.chat.Component
  opaque type Language = net.minecraft.locale.Language
  opaque type MinecraftServer = net.minecraft.server.MinecraftServer

  object Component extends ComponentOps {

    override def literal(text: String): Component =
      net.minecraft.network.chat.Component.literal(text)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[Component] = Try(
      net.minecraft.network.chat.Component.Serializer
        .fromJson(json, server.registryAccess())
    ).toOption

    extension (c: Component) {
      override def translateWith(language: Language): String =
        ServerTranslate.translate(c, language)
      override def serialize(server: MinecraftServer): String =
        net.minecraft.network.chat.Component.Serializer
          .toJson(c, server.registryAccess())
    }
  }

  override def gameRootDirectory: Path =
    Path.fromNioPath(FMLPaths.GAMEDIR.get())

  override def configDirectory: Path =
    Path.fromNioPath(FMLPaths.CONFIGDIR.get())

  override def minecraftVersion: String = "1.21.1"

  override def setOnServerStarting(action: MinecraftServer => Unit): Unit =
    NeoForge.EVENT_BUS.addListener[ServerStartingEvent] { event =>
      action(event.getServer)
    }

  override def setOnServerStopping(action: () => Unit): Unit =
    NeoForge.EVENT_BUS.addListener[ServerStoppingEvent] { _ => action() }

  override def setOnCallingStartCommand(
      action: () => Either[Throwable, Unit]
  ): Unit = InitCommands.startAction = action

  override def setOnCallingStopCommand(action: () => Unit): Unit =
    InitCommands.stopAction = action

  override def setOnCallingDownloadCommand(action: () => Unit): Unit =
    InitCommands.downloadAction = action

  override def setOnBroadcastingMessage(action: Component => Unit): Unit =
    BroadcastingMessage.setCallback(c => action(c))

  object Language extends LanguageOps {
    override def apply(map: String => Option[String]): Language = new Language {
      private val fallback: Language =
        net.minecraft.locale.Language.getInstance()
      override def getOrDefault(s: String, defaultValue: String): String =
        map(s).getOrElse(fallback.getOrDefault(s, defaultValue))
      override def has(s: String): Boolean = map(s).nonEmpty
      override def isDefaultRightToLeft: Boolean = false
      override def getVisualOrder(
          formattedText: FormattedText
      ): FormattedCharSequence =
        FormattedCharSequence.EMPTY
    }

    /** `.lang` format before 1.13 and `.json` format after 1.13 */
    override def parseLanguageFile(
        stream: java.io.InputStream
    ): Option[Map[String, String]] = {
      val map = collection.mutable.HashMap.empty[String, String]
      Try {
        net.minecraft.locale.Language
          .loadFromJson(stream, { (k, v) => map.put(k, v) })
        map.toMap
      }.toOption
    }

    /** `"lang"` before 1.13 and `"json"` after 1.13 */
    override def languageFileExtension: String = "json"
  }

  object MinecraftServer extends MinecraftServerOps {
    extension (server: MinecraftServer) {
      override def broadcastMessage(message: Component): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getPlayerList.broadcastSystemMessage(message, false)
        }
    }
  }
}

export Platform.*
