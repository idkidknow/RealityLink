package com.idkidknow.mcreallink

import cats.Monad
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.ConfigReader
import com.idkidknow.mcreallink.lib.platform.Component
import com.idkidknow.mcreallink.lib.platform.Events
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import com.idkidknow.mcreallink.lib.platform.Platform
import com.idkidknow.mcreallink.server.ApiServer
import com.idkidknow.mcreallink.utils.CallbackBundle
import org.typelevel.log4cats.LoggerFactory

/** run once at the mod init phase */
def modInit[P[_], F[_]: Async: LoggerFactory](
    configReader: ConfigReader[P, F]
)(using Platform[P, F]): F[Unit] = {
  val logger = LoggerFactory[F].getLogger

  // Dispatchers used for running callbacks in mod loader related events.
  // Intentionally leak them because they live as long as the Minecraft game lives
  val leakParallelDispatcher: F[Dispatcher[F]] =
    Dispatcher.parallel[F].allocated.map((d, _) => d)
  val leakSequentialDispatcher: F[Dispatcher[F]] =
    Dispatcher.sequential[F].allocated.map((d, _) => d)

  def modEvents(dispatcher: Dispatcher[F]): F[ModEvents[P, F]] = for {
    onCallingStartCommand <- CallbackBundle
      .combineAll[F, Unit, Either[Throwable, Unit]](
        ().asRight
      ) { cb =>
        Events[P, F].onCallingStartCommand(dispatcher)(() => cb(()))
      }
    onCallingStopCommand <- CallbackBundle[F, Unit] { cb =>
      Events[P, F].onCallingStopCommand(dispatcher)(() => cb(()))
    }
    onBroadcastingMessage <- CallbackBundle[F, P[Component]] { cb =>
      Events[P, F].onBroadcastingMessage(dispatcher)(cb)
    }
  } yield new ModEvents(
    onCallingStartCommand,
    onCallingStopCommand,
    onBroadcastingMessage,
  )

  def onServerStopping(
      dispatcher: Dispatcher[F]
  ): F[CallbackBundle[F, Unit, Unit]] = CallbackBundle[F, Unit] { cb =>
    Events[P, F].onServerStopping(dispatcher)(_ => cb(()))
  }

  for {
    _ <- logger.info("RealityLink mod initializing")
    parallelDispatcher <- leakParallelDispatcher
    sequentialDispatcher <- leakSequentialDispatcher
    modEvents <- modEvents(sequentialDispatcher)
    onServerStopping <- onServerStopping(parallelDispatcher)

    _ <- Events[P, F].onServerStarting(parallelDispatcher) { server =>
      logger.info("Minecraft server starting") *>
        ModMain.make(server, modEvents, configReader).allocated.flatMap {
          (_, finalizer) =>
            def clean: Unit => F[Unit] = { _ =>
              logger.info("Minecraft server stopping") *> finalizer *>
                (onServerStopping -= clean) // We need to cancel the callback to avoid "double free"
            }
            onServerStopping += clean
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
  private val logger = LoggerFactory[F].getLogger
}

object ModMain {
  def make[P[_], F[_]: Monad: LoggerFactory](
      server: P[MinecraftServer],
      modEvents: ModEvents[P, F],
      configReader: ConfigReader[P, F],
  )(using Platform[P, F]): Resource[F, ModMain[P, F]] = {
    val logger = LoggerFactory[F].getLogger

    val apiServer: F[Option[ApiServer]] = configReader.read(server).flatMap {
      case Left(e) =>
        logger.error(e)("failed to read config file") *> None.pure[F]
      case Right(config) => {
        if (config.autoStart) {
          ???
        } else {
          None.pure[F]
        }
      }
    }

    Resource.make(acquire = {
      apiServer.map(ModMain(server, modEvents, configReader, _))
    })(release = ???)
  }
}
