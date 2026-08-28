package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.forge1122.ModEntry
import com.idkidknow.realitylink.forge1122.mixin.BroadcastingMessage
import com.idkidknow.realitylink.forge1122.mixin.LanguageMapMutator
import com.idkidknow.realitylink.forge1122.mixin.ServerTranslate
import com.idkidknow.realitylink.platform.Platform.Component
import com.idkidknow.realitylink.platform.Platform.MinecraftServer
import com.idkidknow.realitylink.platform.api.API
import com.idkidknow.realitylink.platform.api.PlayerInfo
import fs2.io.file.Path
import net.minecraft.stats.StatList
import net.minecraft.stats.StatisticsManagerServer
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.translation.LanguageMap
import net.minecraftforge.common.UsernameCache

import java.io.File
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Try

object Platform extends API {
  opaque type Component = ITextComponent
  opaque type Language = LanguageMap
  opaque type MinecraftServer = net.minecraft.server.MinecraftServer

  object Component extends ComponentOps {

    override def literal(text: String): Component =
      TextComponentString(text)

    override def deserialize(
        json: String,
        server: MinecraftServer,
    ): Option[Component] = Try(
      ITextComponent.Serializer.jsonToComponent(json)
    ).toOption

    extension (c: Component) {
      override def translateWith(language: Language): String =
        ServerTranslate.translate(c, language)
      override def serialize(server: MinecraftServer): String =
        ITextComponent.Serializer.componentToJson(c)
    }
  }

  override def gameRootDirectory: Path =
    Path.fromNioPath(java.io.File(".").toPath.toAbsolutePath)

  override def configDirectory: Path =
    Path.fromNioPath(ModEntry.configDirectory)

  override def minecraftVersion: String = "1.12.2"

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
    override def apply(map: String => Option[String]): Language =
      LanguageMapMutator.make { key =>
        map(key) match {
          case Some(value) => value
          case None => LanguageMapMutator.getDefault.translateKey(key)
        }
      }

    /** `.lang` format before 1.13 and `.json` format after 1.13 */
    override def parseLanguageFile(
        stream: java.io.InputStream
    ): Option[Map[String, String]] = Try {
      import scala.jdk.CollectionConverters.*
      LanguageMap.parseLangFile(stream).asScala.toMap
    }.toOption

    /** `"lang"` before 1.13 and `"json"` after 1.13 */
    override def languageFileExtension: String = "lang"
  }

  object MinecraftServer extends MinecraftServerOps {
    extension (server: MinecraftServer) {
      override def broadcastMessage(message: Component): Unit =
        BroadcastingMessage.ignoreTemporarily { () =>
          server.getPlayerList.sendMessage(message, true)
        }

      override def getStat(uuid: UUID, statName: String): Option[Int] = {
        Option(StatList.getOneShotStat(statName)).flatMap { stat =>
          Option(server.getPlayerList.getPlayerByUUID(uuid))
            .map(_.getStatFile.readStat(stat)) // stats of the online player
            .orElse {
              // read stored stats when player not in server
              val worldDir =
                server.getWorld(0).getSaveHandler.getWorldDirectory.toPath
              val statsFile =
                worldDir.resolve(s"stats/$uuid.json").toFile
              val stats = new StatisticsManagerServer(server, statsFile)
              stats.readStatFile()
              Option(stats.readStat(stat))
            }
        }
      }

      override def getOnlinePlayers: List[PlayerInfo] = {
        server.getPlayerList.getOnlinePlayerProfiles.map { profile =>
          PlayerInfo(profile.getName, profile.getId)
        }.toList
      }

      override def getCachedPlayers: List[PlayerInfo] = {
        UsernameCache.getMap.asScala.map { case (uuid, name) =>
          PlayerInfo(name, uuid)
        }.toList
      }
    }
  }
}

export Platform.*
