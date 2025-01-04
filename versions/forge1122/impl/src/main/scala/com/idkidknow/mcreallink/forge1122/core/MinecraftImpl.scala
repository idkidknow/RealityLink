package com.idkidknow.mcreallink.forge1122.core

import com.idkidknow.mcreallink.api.Minecraft
import com.idkidknow.mcreallink.forge1122.events.{LifecycleEvents, ModCommand, Paths}
import com.idkidknow.mcreallink.forge1122.mixin.complement.{BroadcastingMessage, LanguageMapMutator, ServerTranslate}
import net.minecraft.util.text.{ITextComponent, TextComponentString}
import net.minecraft.util.text.translation.LanguageMap

import java.io.InputStream
import java.nio.file.Path

object MinecraftImpl extends Minecraft {
  override type Component = ITextComponent
  override type Language = LanguageMap
  override type MinecraftServer = net.minecraft.server.MinecraftServer

  override val Component: ComponentClass = new ComponentClass {
    override def translateWith(
        component: ITextComponent,
        language: LanguageMap,
    ): String =
      ServerTranslate.translate(component, language)

    override def literal(text: String): ITextComponent = TextComponentString(
      text
    )

    override def serialize(
        component: ITextComponent,
        server: MinecraftServer,
    ): String =
      ITextComponent.Serializer.componentToJson(component)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[ITextComponent] = try {
      Some(ITextComponent.Serializer.jsonToComponent(json))
    } catch {
      case _: Exception => None
    }
  }
  override val Language: LanguageClass = new LanguageClass {
    override def make(map: String => Option[String]): LanguageMap =
      LanguageMapMutator.make { key =>
        map(key) match {
          case Some(value) => value
          case None => LanguageMapMutator.getDefault.translateKey(key)
        }
      }

    override def parseLanguageFile(
        stream: InputStream
    ): Option[Map[String, String]] = try {
      import scala.jdk.CollectionConverters.*
      Some(LanguageMap.parseLangFile(stream).asScala.toMap)
    } catch {
      case _: Exception => None
    }

    override def languageFileExtension: String = "lang"
  }
  override val MinecraftServer: MinecraftServerClass =
    new MinecraftServerClass {
      override def broadcastMessage(
          server: MinecraftServer,
          message: ITextComponent,
      ): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getPlayerList.sendMessage(message, true)
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

  override def minecraftVersion: String = "1.12.2"
}
