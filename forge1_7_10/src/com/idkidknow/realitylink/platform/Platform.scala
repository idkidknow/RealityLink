package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.forge1710.ModEntry
import com.idkidknow.realitylink.forge1710.mixin.BroadcastingMessage
import com.idkidknow.realitylink.forge1710.mixin.ServerTranslate
import com.idkidknow.realitylink.platform.Platform.Component
import com.idkidknow.realitylink.platform.Platform.MinecraftServer
import com.idkidknow.realitylink.platform.api.API
import fs2.io.file.Path
import net.minecraft.server.MinecraftServer as McMinecraftServer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraft.util.StringTranslate

import scala.util.Try

object Platform extends API {
  opaque type Component = IChatComponent
  opaque type Language = String => Option[String]
  opaque type MinecraftServer = McMinecraftServer

  object Component extends ComponentOps {

    override def literal(text: String): Component =
      ChatComponentText(text)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[Component] = Try(
      IChatComponent.Serializer.func_150699_a(
        json
      ) // IChatComponent.Serializer.jsonToComponent
    ).toOption

    extension (c: Component) {
      override def translateWith(language: Language): String = {
        import scala.jdk.OptionConverters.*
        ServerTranslate.translate(c, key => language(key).toJava)
      }
      override def serialize(server: MinecraftServer): String =
        IChatComponent.Serializer.func_150696_a(
          c
        ) // IChatComponent.Serializer.componentToJson
    }
  }

  override def gameRootDirectory: Path =
    Path.fromNioPath(java.io.File(".").toPath.toAbsolutePath)

  override def configDirectory: Path =
    Path.fromNioPath(ModEntry.configDirectory)

  override def minecraftVersion: String = "1.7.10"

  override def setOnServerStarting(action: MinecraftServer => Unit): Unit =
    ModEntry.serverStartingCallback = s => action(s)

  override def setOnServerStopping(action: () => Unit): Unit =
    ModEntry.serverStoppingCallback = () => action()

  override def setOnCallingStartCommand(
      action: () => Either[Throwable, Unit]
  ): Unit = ModCommand.startAction = action

  override def setOnCallingStopCommand(action: () => Unit): Unit =
    ModCommand.stopAction = action

  override def setOnCallingDownloadCommand(action: () => Unit): Unit =
    ModCommand.downloadAction = action

  override def setOnBroadcastingMessage(action: Component => Unit): Unit =
    BroadcastingMessage.setCallback(c => action(c))

  object Language extends LanguageOps {
    override def apply(map: String => Option[String]): Language = map

    /** `.lang` format before 1.13 and `.json` format after 1.13 */
    override def parseLanguageFile(
        stream: java.io.InputStream
    ): Option[Map[String, String]] = Try {
      import scala.jdk.CollectionConverters.*
      StringTranslate.parseLangFile(stream).asScala.toMap
    }.toOption

    /** `"lang"` before 1.13 and `"json"` after 1.13 */
    override def languageFileExtension: String = "lang"
  }

  object MinecraftServer extends MinecraftServerOps {
    extension (server: MinecraftServer) {
      override def broadcastMessage(message: Component): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getConfigurationManager.sendChatMsg(message)
        }
    }
  }
}

export Platform.*
