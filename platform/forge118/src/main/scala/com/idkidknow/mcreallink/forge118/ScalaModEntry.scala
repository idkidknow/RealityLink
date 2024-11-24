package com.idkidknow.mcreallink.forge118

import com.idkidknow.mcreallink.modInit
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.idkidknow.mcreallink.forge118.platform.Platform
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object ScalaModEntry {
  def init(): Unit = {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    import Platform.given
    modInit[Platform, IO](configReader = ???).unsafeRunAndForget()(using IORuntime.global)
  }
}
