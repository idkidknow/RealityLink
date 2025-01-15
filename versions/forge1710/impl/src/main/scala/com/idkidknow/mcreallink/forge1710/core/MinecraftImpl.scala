package com.idkidknow.mcreallink.forge1710.core

import com.idkidknow.mcreallink.api.Minecraft
import com.idkidknow.mcreallink.forge1710.events.{LifecycleEvents, ModCommand, Paths}
import com.idkidknow.mcreallink.forge1710.mixin.complement.{BroadcastingMessage, ServerTranslate}
import net.minecraft.util.{ChatComponentText, IChatComponent, IChatComponent$Serializer, StringTranslate}
import net.minecraft.server.MinecraftServer as McMinecraftServer

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  override type Component = IChatComponent
  override type Language = String => Option[String]
  override type MinecraftServer = McMinecraftServer

  override val Component: ComponentClass = new ComponentClass {
    override def translateWith(
        component: IChatComponent,
        language: String => Option[String],
    ): String = {
      import scala.jdk.OptionConverters.*
      ServerTranslate.translate(component, key => language(key).toJava)
    }

    override def literal(text: String): IChatComponent =
      ChatComponentText(text)

    override def serialize(
        component: IChatComponent,
        server: McMinecraftServer,
    ): String =
      `IChatComponent$Serializer`.componentToJson(component)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[IChatComponent] = try {
      Some(`IChatComponent$Serializer`.jsonToComponent(json))
    } catch {
      case _: Exception => None
    }
  }
  override val Language: LanguageClass = new LanguageClass {
    override def make(map: String => Option[String]): Language = map

    override def parseLanguageFile(
        stream: InputStream
    ): Option[Map[String, String]] = try {
      import scala.jdk.CollectionConverters.*
      val map = StringTranslate.parseLangFile(stream).asScala.toMap
      Some(map)
    } catch {
      case _: Exception => None
    }

    override def languageFileExtension: String = "lang"
  }
  override val MinecraftServer: MinecraftServerClass =
    new MinecraftServerClass {
      override def broadcastMessage(
          server: MinecraftServer,
          message: IChatComponent,
      ): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getConfigurationManager.sendChatMsg(message)
        }
    }
  override val events: Events = new Events {
    override def setOnServerStarting(action: MinecraftServer => Unit): Unit =
      LifecycleEvents.setServerStartingCallback(server => action(server))

    override def setOnServerStopping(action: () => Unit): Unit =
      LifecycleEvents.setServerStoppingCallback(() => action())

    override def setOnCallingStartCommand(
        action: () => Either[Throwable, Unit]
    ): Unit =
      ModCommand.getInstance().setStartAction { () =>
        action() match {
          case Right(_) => java.util.Optional.empty[Throwable]
          case Left(e) => java.util.Optional.of(e)
        }
      }

    override def setOnCallingStopCommand(action: () => Unit): Unit =
      ModCommand.getInstance().setStopAction(() => action())

    override def setOnCallingDownloadCommand(action: () => Unit): Unit =
      ModCommand.getInstance().setDownloadAction(() => action())

    override def setOnBroadcastingMessage(action: Component => Unit): Unit =
      BroadcastingMessage.setCallback(component => action(component))
  }

  override def gameRootDirectory: Path = Paths.rootDirectory

  override def configDirectory: Path = Paths.configDirectory

  override def minecraftVersion: String = "1.7.10"
}
