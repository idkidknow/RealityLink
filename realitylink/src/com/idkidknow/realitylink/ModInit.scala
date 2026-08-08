package com.idkidknow.realitylink

import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.realitylink.lib.CallbackBundle
import com.idkidknow.realitylink.lib.Leak
import com.idkidknow.realitylink.lib.ServerToml
import com.idkidknow.realitylink.platform.Component
import com.idkidknow.realitylink.platform.MinecraftServer
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.net.Network
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

/** Initialization code
 *
 *  Functions with `F[_]: Leak` should be called at most once when the game
 *  starts.
 */
object ModInit {
  def entry(): Unit = {
    given LoggerFactory[IO] = Slf4jFactory.create[IO]
    init[IO].unsafeRunSync()(using cats.effect.unsafe.IORuntime.global)
  }

  private def init[F[_]: {Async, Leak, LoggerFactory, Files, Network}]
      : F[Unit] = {
    val logger = LoggerFactory[F].getLogger
    for {
      _ <- logger.info("RealityLink mod initializing")

      _ <- ModInit.createDefaultServerToml(
        platform.configDirectory / "realitylink" / "server.toml"
      )

      events <- ModInit.initEvents

      stream: Stream[F, (MinecraftServer, Supervisor[F])] = lib
        .streamMinecraftServer(
          events.serverStarting,
          events.serverStopping,
        )
      // call the main logic every server starting
      _ <- stream
        .evalMap { case (server, supervisor) =>
          ModMain.onServerStarting(
            server,
            supervisor,
            events,
          )
        }
        .compile
        .drain
        .start // start a fiber so we won't block the thread
    } yield ()
  }

  private def createDefaultServerToml[F[_]: {Concurrent, Files}](
      path: Path
  ): F[Unit] = {
    Files[F]
      .exists(path)
      .ifM(
        ifTrue = ().pure[F],
        ifFalse = {
          path.parent
            .map(parent => Files[F].createDirectories(parent))
            .getOrElse(().pure[F]) *> ServerToml.writeDefault(path)
        },
      )
  }

  private[realitylink] final class Events[F[_]](
      val serverStarting: CallbackBundle[F, MinecraftServer, Unit],
      val serverStopping: CallbackBundle[F, Unit, Unit],
      val broadcastingMessage: CallbackBundle[F, Component, Unit],
      val callingStartCommand: CallbackBundle[F, Unit, Either[Throwable, Unit]],
      val callingStopCommand: CallbackBundle[F, Unit, Unit],
      val callingDownloadCommand: CallbackBundle[F, Unit, Unit],
  )

  private def initEvents[F[_]: {Async, Leak}]: F[Events[F]] = for {
    // Leak the dispatcher since its lifetime is as long as the Minecraft
    dispatcher <- Leak[F].leak(Dispatcher.sequential[F])
    serverStarting <- CallbackBundle.fromImpure(
      platform.setOnServerStarting,
      dispatcher.unsafeRunSync,
    )
    serverStopping <- CallbackBundle.fromImpure[F, Unit](
      cb => platform.setOnServerStopping(() => cb(())),
      dispatcher.unsafeRunSync,
    )
    broadcastingMessage <- CallbackBundle.fromImpure(
      platform.setOnBroadcastingMessage,
      dispatcher.unsafeRunAndForget,
    )
    callingStartCommand <- CallbackBundle
      .combineAll[F, Unit, Either[Throwable, Unit]](
        ().asRight
      ) { cb =>
        Async[F].delay {
          platform.setOnCallingStartCommand(() =>
            dispatcher.unsafeRunSync(cb(()))
          )
        }
      }
    callingStopCommand <- CallbackBundle.fromImpure[F, Unit](
      cb => platform.setOnCallingStopCommand(() => cb(())),
      dispatcher.unsafeRunAndForget,
    )
    callingDownloadCommand <- CallbackBundle.fromImpure[F, Unit](
      cb => platform.setOnCallingDownloadCommand(() => cb(())),
      dispatcher.unsafeRunAndForget,
    )
  } yield Events(
    serverStarting,
    serverStopping,
    broadcastingMessage,
    callingStartCommand,
    callingStopCommand,
    callingDownloadCommand,
  )
}
