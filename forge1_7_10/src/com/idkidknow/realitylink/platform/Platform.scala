package com.idkidknow.realitylink.platform

import cats.syntax.all.*
import com.idkidknow.realitylink.forge1710.ModEntry
import com.idkidknow.realitylink.forge1710.mixin.BroadcastingMessage
import com.idkidknow.realitylink.forge1710.mixin.ServerTranslate
import com.idkidknow.realitylink.platform.Platform.Component
import com.idkidknow.realitylink.platform.Platform.MinecraftServer
import com.idkidknow.realitylink.platform.api.API
import com.idkidknow.realitylink.platform.api.PlayerInfo
import fs2.io.file.Path
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer as McMinecraftServer
import net.minecraft.stats.StatList
import net.minecraft.stats.StatisticsFile
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraft.util.StringTranslate
import net.minecraftforge.common.UsernameCache

import java.io.File
import java.util.UUID
import scala.jdk.CollectionConverters.*
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

      override def getStat(uuid: UUID, statName: String): Option[Int] = {
        Option(StatList.func_151177_a(statName)).flatMap { stat =>
          server.getConfigurationManager.playerEntityList.asScala
            .collectFirst {
              case p: EntityPlayerMP if p.getUniqueID === uuid => p
            }
            .map(
              _.func_147099_x.writeStat(stat)
            ) // stats of the online player
            .orElse {
              // read stored stats when player not in server
              val worldDir = server
                .worldServerForDimension(0)
                .getSaveHandler
                .getWorldDirectory
                .toPath
              val statsFile = worldDir.resolve(s"stats/$uuid.json").toFile
              val stats = new StatisticsFile(server, statsFile)
              stats.func_150882_a()
              Option(stats.writeStat(stat))
            }
        }
      }

      override def getOnlinePlayers: List[PlayerInfo] = {
        server.getConfigurationManager.func_152600_g.map { profile =>
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
