package com.idkidknow.mcreallink.forge1165.core

import scala.annotation.unused

@unused
object ScalaEntry {
  @unused
  def entry(): Unit = {
    com.idkidknow.mcreallink.ModInit.entryWithSlf4j(MinecraftImpl)
    InitCommands.init()
  }
}
