package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.ModInit

object Entry {
  def entry(): Unit = {
    InitCommands.init()
    ModInit.entry()
  }
}
