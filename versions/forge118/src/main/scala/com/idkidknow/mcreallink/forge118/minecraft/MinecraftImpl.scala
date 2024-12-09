package com.idkidknow.mcreallink.forge118.minecraft

import com.idkidknow.mcreallink.forge118.mixin.complement.{BroadcastingMessage, ServerTranslate}
import com.idkidknow.mcreallink.minecraft.Minecraft
import net.minecraft.network.chat.{ChatType, FormattedText, TextComponent, Component as McComponent}
import net.minecraft.locale.Language as McLanguage
import net.minecraft.server.MinecraftServer as McMinecraftServer
import net.minecraft.util.FormattedCharSequence
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.server.{ServerStartingEvent, ServerStoppingEvent}
import net.minecraftforge.fml.loading.FMLPaths

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  /** [[net.minecraft.network.chat.Component]] */
  override type Component = McComponent
  /** [[net.minecraft.locale.Language]] */
  override type Language = McLanguage
  /** [[net.minecraft.server.MinecraftServer]] */
  override type MinecraftServer = McMinecraftServer
  override val Component: MinecraftImpl.ComponentClass = new ComponentClass {
    override def translateWith(component: Component, language: Language): String =
      ServerTranslate.translate(component, language)
    override def literal(text: String): Component = TextComponent(text)
    override def serialize(component: Component, server: MinecraftServer): String =
      McComponent.Serializer.toJson(component)
    override def deserialize(json: String, server: MinecraftServer): Option[Component] = try {
      Some(McComponent.Serializer.fromJson(json))
    } catch {
      case _: Exception => None
    }
  }
  override val Language: MinecraftImpl.LanguageClass = new LanguageClass {
    override def get(language: McLanguage, key: String): Option[String] =
      if (language.has(key)) Some(language.getOrDefault(key)) else None
    override def make(map: String => Option[String]): McLanguage = new McLanguage {
      override def getOrDefault(id: String): String = map(id).getOrElse(id)
      override def has(id: String): Boolean = map(id).isDefined
      // only used in GUI and has no effects on server
      override def isDefaultRightToLeft: Boolean = false
      // only used in GUI
      override def getVisualOrder(text: FormattedText): FormattedCharSequence = FormattedCharSequence.EMPTY
    }
    override def classLoaderResourceStream(path: String): InputStream =
      classOf[McLanguage].getClassLoader.getResourceAsStream(path)
    override def parseLanguageFile(stream: InputStream): Option[Map[String, String]] = {
      val map = scala.collection.mutable.HashMap[String, String]()
      try {
        McLanguage.loadFromJson(stream, map.put)
        Some(map.toMap)
      } catch {
        case _: Exception => None
      }
    }
  }
  override val MinecraftServer: MinecraftImpl.MinecraftServerClass = new MinecraftServerClass {
    override def resourceNamespaces(server: McMinecraftServer): Iterable[String] = {
      import scala.jdk.CollectionConverters.*
      server.getResourceManager.getNamespaces.asScala
    }
    override def broadcastMessage(server: McMinecraftServer, message: McComponent): Unit = {
      BroadcastingMessage.ignoreTemporarily { () =>
        server.getPlayerList.broadcastMessage(message, ChatType.SYSTEM, net.minecraft.Util.NIL_UUID)
      }
    }
  }

  override val events: MinecraftImpl.Events = new Events {
    override def onServerStarting(action: McMinecraftServer => Unit): Unit = {
      MinecraftForge.EVENT_BUS.addListener[ServerStartingEvent] { event =>
        action(event.getServer)
      }
    }
    override def onServerStopping(action: McMinecraftServer => Unit): Unit = {
      MinecraftForge.EVENT_BUS.addListener[ServerStoppingEvent] { event =>
        action(event.getServer)
      }
    }
    override def setOnCallingStartCommand(action: () => Either[Throwable, Unit]): Unit =
      InitCommands.startAction = action
    override def setOnCallingStopCommand(action: () => Unit): Unit =
      InitCommands.stopAction = action
    override def setOnBroadcastingMessage(action: McComponent => Unit): Unit = {
      BroadcastingMessage.callback = action
    }
  }

  override def gameRootDirectory: Path = FMLPaths.GAMEDIR.get()

  override def configDirectory: Path = FMLPaths.CONFIGDIR.get()
}
