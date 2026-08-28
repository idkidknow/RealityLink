package com.idkidknow.realitylink.platform.api

import io.circe.Codec

import java.util.UUID

final case class PlayerInfo(name: String, uuid: UUID) derives Codec

trait MinecraftServerAPI extends Types {
  trait MinecraftServerOps {
    extension (server: MinecraftServer) {
      def broadcastMessage(message: Component): Unit
      def getStat(uuid: UUID, statName: String): Option[Int]
      def getOnlinePlayers: List[PlayerInfo]
      def getCachedPlayers: List[PlayerInfo]
    }
  }

  val MinecraftServer: MinecraftServerOps
}
