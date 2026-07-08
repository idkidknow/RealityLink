package com.idkidknow.realitylink.platform

import com.idkidknow.realitylink.ModInit
import com.idkidknow.realitylink.forge1710.ModEntry

object Entry {
  def entry(): Unit = {
    ModEntry.command = ModCommand
    ModInit.entry()
  }
}
