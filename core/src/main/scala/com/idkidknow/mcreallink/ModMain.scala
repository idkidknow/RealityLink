package com.idkidknow.mcreallink

import cats.effect.kernel.Async
import cats.effect.kernel.Fiber
import cats.effect.kernel.MonadCancel
import cats.effect.kernel.Ref
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.AssetDownload
import com.idkidknow.mcreallink.lib.CallbackBundle
import com.idkidknow.mcreallink.lib.GameChat
import com.idkidknow.mcreallink.lib.ModConfig
import com.idkidknow.mcreallink.minecraft.Minecraft
import com.idkidknow.mcreallink.server.ChatInterface
import com.idkidknow.mcreallink.server.RealityLinkServer
import de.lhns.fs2.compress.Archiver
import de.lhns.fs2.compress.Unarchiver
import de.lhns.fs2.compress.ZipArchiver
import de.lhns.fs2.compress.ZipUnarchiver
import fs2.io.file.Files
import fs2.io.file.Path
import org.http4s.netty.client.NettyClientBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory

import java.util.zip.ZipEntry
import scala.concurrent.duration.*

object ModMain {
  private[mcreallink] def onServerStarting[F[_]: Async: LoggerFactory: Files](
      using mc: Minecraft
  )(
      server: mc.MinecraftServer,
      supervisor: Supervisor[F],
      events: ModInit.Events[F, mc.type],
  ): F[Unit] = {
    given logger: Logger[F] = LoggerFactory[F].getLogger
    given Unarchiver[F, Option, ZipEntry] = ZipUnarchiver.make[F]()

    type RunningServer = Fiber[F, Throwable, Nothing]

    def tryAutoStart(ref: Ref[F, Option[RunningServer]]): F[Unit] = {
      val config = ModConfig.fromConfigFile
      config.flatMap {
        case Left(e) => logger.warn(e)("failed to load config")
        case Right(config) =>
          if (config.autoStart) {
            logger.info("autoStart = true") *>
              runRealityLinkServer(
                server,
                supervisor,
                events.broadcastingMessage,
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
            val config = ModConfig.fromConfigFile
            config.flatMap {
              case Left(e) =>
                logger.error(e)("failed to load config") *> e.asLeft.pure[F]
              case Right(config) =>
                {
                  runRealityLinkServer(
                    server,
                    supervisor,
                    events.broadcastingMessage,
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
        .supervise(
          events.callingStartCommand.registerAsResource(callback).useForever
        )
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
        .supervise(
          events.callingStopCommand.registerAsResource(callback).useForever
        )
        .void
    }

    def registerDownloadCommand: F[Unit] = {
      val callback: Unit => F[Unit] = { _ =>
        val download = NettyClientBuilder[F].resource.use { client =>
          val target = Path.fromNioPath(
            mc.gameRootDirectory
          ) / "serverlang" / "vanilla.zip"
          given Archiver[F, Option] = ZipArchiver.makeDeflated()
          AssetDownload
            .downloadLanguageAssets(mc.minecraftVersion, Path("."), client)
            .flatMap {
              case Right(_) =>
                logger.info(
                  show"Successfully downloaded language assets $target"
                )
              case Left(e) =>
                logger.error(e)(
                  show"Failed to download language assets: ${e.getMessage}"
                )
            }
        }
        val withTimeout = Async[F].timeoutTo(
          download,
          30.seconds,
          logger.error("Downloading timed out"),
        )
        logger.info("Start downloading language assets") *>
          supervisor.supervise(withTimeout).void
      }
      supervisor
        .supervise(
          events.callingDownloadCommand.registerAsResource(callback).useForever
        )
        .void
    }

    Ref.of(Option.empty[RunningServer]).flatMap { realityLinkServer =>
      tryAutoStart(realityLinkServer) *>
        registerStartCommand(realityLinkServer) *>
        registerStopCommand(realityLinkServer) *>
        registerDownloadCommand
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
    val interface: ChatInterface[F] =
      ChatInterface[F](gameChat, config.language)
    val realityLinkServer: F[Nothing] = {
      val serverR = RealityLinkServer
        .netty[F]
        .run(interface, config.serverConfig)

      logger.info(
        show"Starting RealityLink server on ${config.serverConfig.host}:${config.serverConfig.port}"
      ) *> serverR.useForever
    }

    supervisor.supervise {
      MonadCancel[F].onCancel(
        realityLinkServer,
        logger.info("RealityLink server stopped"),
      )
    }
  }

}
