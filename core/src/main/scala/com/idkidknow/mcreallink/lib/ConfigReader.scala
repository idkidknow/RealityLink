package com.idkidknow.mcreallink.lib

import com.idkidknow.mcrealcomm.ModConfig
import com.idkidknow.mcreallink.lib.platform.MinecraftServer

trait ConfigReader[P[_], F[_]] {
  def read(server: P[MinecraftServer]): F[ModConfig[P]]
}
