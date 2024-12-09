package com.idkidknow.mcreallink.forge118

import com.idkidknow.mcreallink.entry
import cats.effect.IO
import com.idkidknow.mcreallink.forge118.minecraft.MinecraftImpl
import org.typelevel.log4cats.slf4j.Slf4jFactory

object ScalaModEntry {
  def init(): Unit = {
    entry(MinecraftImpl, Slf4jFactory.create[IO])
  }
}
