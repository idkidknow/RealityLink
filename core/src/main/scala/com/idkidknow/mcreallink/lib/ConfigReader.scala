package com.idkidknow.mcreallink.lib

import com.idkidknow.mcreallink.ModConfig
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import java.io.IOException

trait ConfigReader[P[_], F[_]] {
  def read(server: P[MinecraftServer]): F[Either[ConfigReadingException, ModConfig[P]]]
}

enum ConfigReadingException extends Exception {
  case IO(e: IOException)
  case Parsing(e: Exception)
  case Format(message: String)
}
