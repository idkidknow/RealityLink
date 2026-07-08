package com.idkidknow.realitylink.platform.api

trait MinecraftServerAPI extends Types {
  trait MinecraftServerOps {
    extension (server: MinecraftServer) {
      def broadcastMessage(message: Component): Unit
    }
  }

  val MinecraftServer: MinecraftServerOps
}
