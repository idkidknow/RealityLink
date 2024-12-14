package com.idkidknow.mcreallink.forge1710

import com.idkidknow.mcreallink.forge1710.mixin.complement.{BroadcastingMessage, ServerTranslate}
import com.idkidknow.mcreallink.minecraft.Minecraft
import net.minecraft.server.MinecraftServer as McMinecraftServer
import net.minecraft.util.{ChatComponentText, IChatComponent, StringTranslate}
import net.minecraft.util.IChatComponent$Serializer

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  override type Component = IChatComponent
  override type Language = String => Option[String]
  override type MinecraftServer = McMinecraftServer
  override val Component: MinecraftImpl.ComponentClass = new ComponentClass {
    override def translateWith(component: IChatComponent, language: Language): String = {
      import scala.jdk.OptionConverters.*
      ServerTranslate.translate(component, { key => language(key).toJava })
    }
    override def literal(text: String): IChatComponent =
      ChatComponentText(text)
    override def serialize(component: IChatComponent, server: McMinecraftServer): String =
      `IChatComponent$Serializer`.componentToJson(component)
    override def deserialize(json: String, server: McMinecraftServer): Option[IChatComponent] = try {
      Some(`IChatComponent$Serializer`.jsonToComponent(json))
    } catch {
      case _: Exception => None
    }
  }
  override val Language: MinecraftImpl.LanguageClass = new LanguageClass {
    override def make(map: String => Option[String]): Language = map
    override def parseLanguageFile(stream: InputStream): Option[Map[String, String]] = try {
      import scala.jdk.CollectionConverters.*
      val map = StringTranslate.parseLangFile(stream).asScala.toMap
      Some(map)
    } catch {
      case _: Exception => None
    }
    override def languageFileExtension: String = "lang"
  }
  override val MinecraftServer: MinecraftImpl.MinecraftServerClass = new MinecraftServerClass {
    override def broadcastMessage(server: McMinecraftServer, message: IChatComponent): Unit =
      BroadcastingMessage.ignoreTemporarily { () =>
        server.getConfigurationManager.sendChatMsg(message)
      }
  }
  override val events: MinecraftImpl.Events = new Events {
    override def setOnServerStarting(action: McMinecraftServer => Unit): Unit = {
      LifecycleEvents.serverStartingCallback = action
    }

    override def setOnServerStopping(action: () => Unit): Unit = {
      LifecycleEvents.serverStoppingCallback = action
    }

    override def setOnCallingStartCommand(action: () => Either[Throwable, Unit]): Unit = {
      ModCommand.getInstance().setStartAction { sender =>
        action() match {
          case Left(e) => sender.addChatMessage(ChatComponentText("failed: " + e.getMessage))
          case Right(_) => sender.addChatMessage(ChatComponentText("ok"))
        }
      }
    }

    override def setOnCallingStopCommand(action: () => Unit): Unit = {
      ModCommand.getInstance().setStopAction { sender =>
        action()
        sender.addChatMessage(ChatComponentText("ok"))
      }
    }

    override def setOnCallingDownloadCommand(action: () => Unit): Unit = {
      ModCommand.getInstance().setDownloadAction { sender =>
        action()
        sender.addChatMessage(ChatComponentText("start to download..."))
      }
    }

    override def setOnBroadcastingMessage(action: IChatComponent => Unit): Unit = {
      BroadcastingMessage.callback = action
    }
  }

  override def gameRootDirectory: Path = java.io.File(".").toPath.toAbsolutePath
  override def configDirectory: Path = ModEntry.configDirectory

  override def minecraftVersion: String = "1.7.10"
}
