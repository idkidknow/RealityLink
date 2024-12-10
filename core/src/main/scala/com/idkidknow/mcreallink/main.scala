package com.idkidknow.mcreallink

import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.effect.kernel.Fiber
import cats.effect.kernel.Ref
import cats.effect.std.Dispatcher
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.GameChat
import com.idkidknow.mcreallink.lib.Leak
import com.idkidknow.mcreallink.lib.MinecraftServer
import com.idkidknow.mcreallink.lib.ModConfig
import com.idkidknow.mcreallink.minecraft.Minecraft
import com.idkidknow.mcreallink.server.ChatInterface
import com.idkidknow.mcreallink.server.RealityLinkServer
import com.idkidknow.mcreallink.utils.CallbackBundle
import de.lhns.fs2.compress.Unarchiver
import de.lhns.fs2.compress.ZipUnarchiver
import fs2.Stream
import fs2.io.file.Files
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import java.util.zip.ZipEntry

def entry(mc: Minecraft, loggerFactory: LoggerFactory[IO]): Unit = {
  given Minecraft = mc
  given LoggerFactory[IO] = loggerFactory
  import cats.effect.unsafe.implicits.global
  modInit[IO].unsafeRunSync()
}

def modInit[F[_]: Async: Leak: LoggerFactory: Files](using
    mc: Minecraft
): F[Unit] = {
  val logger = LoggerFactory[F].getLogger
  for {
    _ <- logger.info("RealityLink mod initializing")

    dispatcher <- Leak[F].leak(Dispatcher.sequential[F])
    serverStarting <- CallbackBundle.fromImpure(
      mc.events.onServerStarting,
      dispatcher.unsafeRunSync,
    )
    serverStopping <- CallbackBundle.fromImpure(
      mc.events.onServerStopping,
      dispatcher.unsafeRunSync,
    )
    broadcastingMessage <- CallbackBundle.fromImpure(
      mc.events.setOnBroadcastingMessage,
      dispatcher.unsafeRunAndForget,
    )
    callingStartCommand <- CallbackBundle
      .combineAll[F, Unit, Either[Throwable, Unit]](
        ().asRight
      ) { cb =>
        Async[F].delay {
          mc.events.setOnCallingStartCommand(() =>
            dispatcher.unsafeRunSync(cb(()))
          )
        }
      }
    callingStopCommand <- CallbackBundle.fromImpure[F, Unit](
      cb => mc.events.setOnCallingStopCommand(() => cb(())),
      dispatcher.unsafeRunAndForget,
    )

    s: Stream[F, (mc.MinecraftServer, Supervisor[F])] = MinecraftServer.stream(
      serverStarting,
      serverStopping,
    )
    _ <- s
      .evalMap { case (server, supervisor) =>
        onServerStarting(
          server,
          supervisor,
          broadcastingMessage,
          callingStartCommand,
          callingStopCommand,
        )
      }
      .compile
      .drain
      .start
  } yield ()
}

def onServerStarting[F[_]: Async: LoggerFactory: Files](using
    mc: Minecraft
)(
    server: mc.MinecraftServer,
    supervisor: Supervisor[F],
    broadcastingMessage: CallbackBundle[F, mc.Component, Unit],
    callingStartCommand: CallbackBundle[F, Unit, Either[Throwable, Unit]],
    callingStopCommand: CallbackBundle[F, Unit, Unit],
): F[Unit] = {
  given logger: Logger[F] = LoggerFactory[F].getLogger
  given Unarchiver[F, Option, ZipEntry] = ZipUnarchiver.make[F]()

  type RunningServer = Fiber[F, Throwable, Nothing]

  def tryAutoStart(ref: Ref[F, Option[RunningServer]]): F[Unit] = {
    val config = ModConfig.fromConfigFile(server)
    config.flatMap {
      case Left(e) => logger.warn(e)("failed to load config")
      case Right(config) =>
        if (config.autoStart) {
          logger.info("autoStart = true") *>
            runRealityLinkServer(
              server,
              supervisor,
              broadcastingMessage,
              config,
            ).flatMap { runningServer =>
              ref.set(Some(runningServer))
            }
        } else {
          ().pure[F]
        }
    }
  }

  def registerStartCommand(ref: Ref[F, Option[RunningServer]]): F[Unit] = {
    val callback: Unit => F[Either[Throwable, Unit]] = { _ =>
      ref.get.flatMap {
        case Some(_) =>
          RuntimeException("Server already started").asLeft[Unit].pure[F]
        case None => {
          val config = ModConfig.fromConfigFile(server)
          config.flatMap {
            case Left(e) =>
              logger.error(e)("failed to load config") *> e.asLeft.pure[F]
            case Right(config) =>
              {
                runRealityLinkServer(
                  server,
                  supervisor,
                  broadcastingMessage,
                  config,
                )
              }.flatMap { runningServer =>
                ref.set(Some(runningServer)) *> ().asRight.pure[F]
              }
          }
        }
      }
    }
    supervisor
      .supervise(callingStartCommand.registerAsResource(callback).useForever)
      .void
  }

  def registerStopCommand(ref: Ref[F, Option[RunningServer]]): F[Unit] = {
    val callback: Unit => F[Unit] = { _ =>
      ref.get.flatMap {
        case None => ().pure[F]
        case Some(runningServer) =>
          runningServer.cancel *> ref.set(None)
      }
    }
    supervisor
      .supervise(callingStopCommand.registerAsResource(callback).useForever)
      .void
  }

  Ref.of(Option.empty[RunningServer]).flatMap { realityLinkServer =>
    tryAutoStart(realityLinkServer) *>
      registerStartCommand(realityLinkServer) *>
      registerStopCommand(realityLinkServer)
  }
}

def runRealityLinkServer[F[_]: Async: LoggerFactory](using
    mc: Minecraft
)(
    server: mc.MinecraftServer,
    supervisor: Supervisor[F],
    broadcastingMessage: CallbackBundle[F, mc.Component, Unit],
    config: ModConfig,
): F[Fiber[F, Throwable, Nothing]] = {
  val logger = LoggerFactory[F].getLogger

  val gameChat: GameChat[F] = GameChat[F](server, broadcastingMessage)
  val interface: ChatInterface[F] = ChatInterface[F](gameChat, config.language)
  val realityLinkServer: F[Nothing] =
    logger.info(
      show"Starting RealityLink server on ${config.serverConfig.host}:${config.serverConfig.port}"
    ) *>
      RealityLinkServer.netty[F].run(interface, config.serverConfig).useForever
  supervisor.supervise(realityLinkServer)
}
