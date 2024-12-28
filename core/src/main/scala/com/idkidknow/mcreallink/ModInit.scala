package com.idkidknow.mcreallink

import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.CallbackBundle
import com.idkidknow.mcreallink.lib.Leak
import com.idkidknow.mcreallink.lib.MinecraftServer
import com.idkidknow.mcreallink.lib.ServerToml
import com.idkidknow.mcreallink.api.Minecraft
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import org.typelevel.log4cats.LoggerFactory

/** Initialization code
 *
 *  Functions with `F[_]: Leak` should be called at most once when the game
 *  starts.
 */
object ModInit {
  def entry(mc: Minecraft, loggerFactory: LoggerFactory[IO]): Unit = {
    given Minecraft = mc
    given LoggerFactory[IO] = loggerFactory
    import cats.effect.unsafe.implicits.global
    init[IO].unsafeRunSync()
  }

  def entryWithSlf4j(mc: Minecraft): Unit = {
    import org.typelevel.log4cats.slf4j.Slf4jFactory
    entry(mc, Slf4jFactory.create[IO])
  }

  def init[F[_]: Async: Leak: LoggerFactory: Files](using
      mc: Minecraft
  ): F[Unit] = {
    val logger = LoggerFactory[F].getLogger
    for {
      _ <- logger.info("RealityLink mod initializing")

      _ <- ModInit.createDefaultServerToml(
        Path.fromNioPath(mc.configDirectory) / "reallink" / "server.toml"
      )

      events <- ModInit.initEvents

      s: Stream[F, (mc.MinecraftServer, Supervisor[F])] = MinecraftServer
        .stream(
          events.serverStarting,
          events.serverStopping,
        )
      _ <- s
        .evalMap { case (server, supervisor) =>
          ModMain.onServerStarting(
            server,
            supervisor,
            events,
          )
        }
        .compile
        .drain
        .start
    } yield ()
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def createDefaultServerToml[F[_]: Concurrent: Files](path: Path): F[Unit] = {
    Files[F]
      .exists(path)
      .ifM(
        ifTrue = ().pure[F],
        ifFalse = Stream
          .emit(ServerToml.defaultTomlString)
          .through(fs2.text.utf8.encode)
          .through(Files[F].writeAll(path))
          .compile
          .drain,
      )
  }

  private[mcreallink] final class Events[F[_], MC <: Minecraft](val mc: MC)(
      val serverStarting: CallbackBundle[F, mc.MinecraftServer, Unit],
      val serverStopping: CallbackBundle[F, Unit, Unit],
      val broadcastingMessage: CallbackBundle[F, mc.Component, Unit],
      val callingStartCommand: CallbackBundle[F, Unit, Either[Throwable, Unit]],
      val callingStopCommand: CallbackBundle[F, Unit, Unit],
      val callingDownloadCommand: CallbackBundle[F, Unit, Unit],
  )

  private def initEvents[F[_]: Async: Leak](using
      mc: Minecraft
  ): F[Events[F, mc.type]] = for {
    // Leak the dispatcher since its lifetime is as long as the Minecraft
    dispatcher <- Leak[F].leak(Dispatcher.sequential[F])
    serverStarting <- CallbackBundle.fromImpure(
      mc.events.setOnServerStarting,
      dispatcher.unsafeRunSync,
    )
    serverStopping <- CallbackBundle.fromImpure[F, Unit](
      cb => mc.events.setOnServerStopping(() => cb(())),
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
    callingDownloadCommand <- CallbackBundle.fromImpure[F, Unit](
      cb => mc.events.setOnCallingDownloadCommand(() => cb(())),
      dispatcher.unsafeRunAndForget,
    )
  } yield Events(mc)(
    serverStarting,
    serverStopping,
    broadcastingMessage,
    callingStartCommand,
    callingStopCommand,
    callingDownloadCommand,
  )
}
