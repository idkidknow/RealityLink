package com.idkidknow.mcreallink.forge1192.core

object ScalaEntry {
  def entry(): Unit = {
    com.idkidknow.mcreallink.ModInit.entryWithSlf4j(MinecraftImpl)
    InitCommands.init()
  }
}
