package com.idkidknow.realitylink.platform.api

import java.util.UUID

trait MinecraftServerAPI extends Types {
  trait MinecraftServerOps {
    extension (server: MinecraftServer) {
      def broadcastMessage(message: Component): Unit
      def getStat(uuid: UUID, statName: String): Option[Int]
    }
  }

  val MinecraftServer: MinecraftServerOps
}
