package com.idkidknow.mcreallink

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import com.idkidknow.mcrealcomm.ModEvents
import com.idkidknow.mcreallink.lib.platform.Component
import com.idkidknow.mcreallink.lib.platform.Events
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import com.idkidknow.mcreallink.lib.platform.Platform
import com.idkidknow.mcreallink.utils.CallbackBundle
import org.typelevel.log4cats.LoggerFactory
import com.idkidknow.mcreallink.lib.ConfigReader
import cats.Monad
import com.idkidknow.mcreallink.server.ApiServer

def modInit[P[_], F[_]: Concurrent: LoggerFactory](
  configReader: ConfigReader[P, F],
)(using Platform[P, F]): F[Unit] = {
  val logger = LoggerFactory[F].getLogger
  for {
    onServerStopping <- CallbackBundle[F, Unit] { cb =>
      Events[P, F].onServerStopping(_ => cb(()))
    }

    onCallingStartCommand <- CallbackBundle.combineAll[F, Unit, Either[Throwable, Unit]](
      ().asRight,
    ) { cb =>
      Events[P, F].onCallingStartCommand(() => cb(()))
    }
    onCallingStopCommand <- CallbackBundle[F, Unit] { cb =>
      Events[P, F].onCallingStopCommand(() => cb(()))
    }
    onBroadcastingMessage <- CallbackBundle[F, P[Component]] { cb =>
      Events[P, F].onBroadcastingMessage(cb)
    }
    modEvents = ModEvents(
      onCallingStartCommand,
      onCallingStopCommand,
      onBroadcastingMessage,
    )

    _ <- logger.info("RealityLink mod initializing")

    _ <- Events[P, F].onServerStarting { server =>
      logger.info("Minecraft server starting") *>
        ModMain.make(server, modEvents, configReader).flatMap { modMain =>
          def clean: Unit => F[Unit] = { _ =>
            logger.info("Minecraft server stopping") *> modMain.clean *>
              (onServerStopping - clean) // We need to cancel the callback to avoid "double free"
          }
          onServerStopping + clean
        }
    }

  } yield ()
}

final class ModMain[P[_], F[_]: LoggerFactory] private (
    val server: P[MinecraftServer],
    val modEvents: ModEvents[P, F],
    val configReader: ConfigReader[P, F],
    val apiServer: Option[ApiServer],
) {
  val logger = LoggerFactory[F].getLogger
  def clean: F[Unit] = ???
}

object ModMain {
  def make[P[_], F[_]: Monad: LoggerFactory](
      server: P[MinecraftServer],
      modEvents: ModEvents[P, F],
      configReader: ConfigReader[P, F],
  )(using Platform[P, F]): F[ModMain[P, F]] = {
    val config = configReader.read(server)
    config.flatMap { config =>
      if (config.autoStart) {
        ???
      } else {
        ModMain(server, modEvents, configReader, None).pure[F]
      }
    }
  }
}
