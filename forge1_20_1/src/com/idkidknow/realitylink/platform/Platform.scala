package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.forge1201.mixin.BroadcastingMessage
import com.idkidknow.realitylink.forge1201.mixin.ServerTranslate
import com.idkidknow.realitylink.platform.Platform.Component
import com.idkidknow.realitylink.platform.Platform.MinecraftServer
import com.idkidknow.realitylink.platform.api.API
import fs2.io.file.Path
import net.minecraft.network.chat.FormattedText
import net.minecraft.stats.ServerStatsCounter
import net.minecraft.stats.Stat
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.fml.loading.FMLPaths

import java.util.UUID
import scala.jdk.OptionConverters.*
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
      net.minecraft.network.chat.Component.Serializer.fromJson(json)
    ).toOption

    extension (c: Component) {
      override def translateWith(language: Language): String =
        ServerTranslate.translate(c, language)
      override def serialize(server: MinecraftServer): String =
        net.minecraft.network.chat.Component.Serializer.toJson(c)
    }
  }

  override def gameRootDirectory: Path =
    Path.fromNioPath(FMLPaths.GAMEDIR.get())

  override def configDirectory: Path =
    Path.fromNioPath(FMLPaths.CONFIGDIR.get())

  override def minecraftVersion: String = "1.20.1"

  override def setOnServerStarting(action: MinecraftServer => Unit): Unit =
    MinecraftForge.EVENT_BUS.addListener[ServerStartingEvent] { event =>
      action(event.getServer)
    }

  override def setOnServerStopping(action: () => Unit): Unit =
    MinecraftForge.EVENT_BUS.addListener[ServerStoppingEvent] { _ => action() }

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

      override def getStat(uuid: UUID, statName: String): Option[Int] = {
        ObjectiveCriteria
          .byName(statName)
          .toScala
          .collect { case s: Stat[?] =>
            s
          }
          .flatMap { stat =>
            Option(server.getPlayerList.getPlayer(uuid))
              .map(_.getStats.getValue(stat)) // stats of the online player
              .orElse {
                // read stored stats when player not in server
                val statsFile =
                  server
                    .getWorldPath(LevelResource.PLAYER_STATS_DIR)
                    .resolve(s"$uuid.json")
                    .toFile
                val counter = new ServerStatsCounter(server, statsFile)
                Option(counter.getValue(stat))
              }
          }
      }
    }
  }
}

export Platform.*
