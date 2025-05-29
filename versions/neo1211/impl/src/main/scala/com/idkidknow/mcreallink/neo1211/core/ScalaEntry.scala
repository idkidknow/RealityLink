package com.idkidknow.mcreallink.neo1211.core

object ScalaEntry {
  def entry(): Unit = {
    com.idkidknow.mcreallink.ModInit.entryWithSlf4j(MinecraftImpl)
    InitCommands.init()
  }
}
