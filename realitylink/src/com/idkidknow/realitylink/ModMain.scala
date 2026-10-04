package com.idkidknow.realitylink

import cats.effect.kernel.Async
import cats.effect.kernel.Fiber
import cats.effect.kernel.MonadCancel
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.realitylink.lib.AssetDownload
import com.idkidknow.realitylink.lib.CallbackBundle
import com.idkidknow.realitylink.lib.ModConfig
import com.idkidknow.realitylink.lib.ServerToml
import com.idkidknow.realitylink.platform.Component
import com.idkidknow.realitylink.platform.MinecraftServer
import com.idkidknow.realitylink.server.ChatInterface
import com.idkidknow.realitylink.server.RealityLinkServer
import de.lhns.fs2.compress.Archiver
import de.lhns.fs2.compress.ZipArchiver
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory

import scala.concurrent.duration.*

object ModMain {
  private[realitylink] def realityLinkMain[
      F[_]: {Async, LoggerFactory, Files, Network}
  ](server: MinecraftServer, events: ModInit.Events[F]): Resource[F, Unit] = {
    given logger: Logger[F] = LoggerFactory[F].getLogger

    type RunningServer = Fiber[F, Throwable, Nothing]

    for {
      supervisor <- Supervisor[F]
      serverRef <- Resource.eval(Ref.of[F, Option[RunningServer]](None))

      // register `start` command
      _ <- {
        val callback: Unit => F[Either[Throwable, Unit]] = { _ =>
          serverRef.get.flatMap {
            case Some(_) =>
              RuntimeException("Server already started").asLeft[Unit].pure[F]
            case None =>
              val config = ModConfig.fromConfigFile
              config.flatMap {
                case Left(e) =>
                  logger.error(e)("failed to load config") *> e.asLeft.pure[F]
                case Right(config) =>
                  runRealityLinkServer(
                    server,
                    supervisor,
                    events.broadcastingMessage,
                    config,
                  ).flatMap { runningServer =>
                    serverRef.set(Some(runningServer)) *> ().asRight.pure[F]
                  }
              }
          }
        }
        events.callingStartCommand.registerAsResource(callback)
      }

      // register `stop` command
      _ <- {
        val callback: Unit => F[Unit] = { _ =>
          serverRef.get.flatMap {
            case None => ().pure[F]
            case Some(runningServer) =>
              runningServer.cancel *> serverRef.set(None)
          }
        }
        events.callingStopCommand.registerAsResource(callback)
      }

      // register `download` command
      _ <- {
        val callback: Unit => F[Unit] = { _ =>
          val download = EmberClientBuilder.default[F].build.use { client =>
            val parentPath = platform.gameRootDirectory / "serverlang"
            val createParent: F[Unit] = Files[F].createDirectories(parentPath)
            val target = parentPath / "vanilla.zip"
            given Archiver[F, Option] = ZipArchiver.makeDeflated()
            createParent *> AssetDownload
              .downloadLanguageAssets(platform.minecraftVersion, target, client)
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
        events.callingDownloadCommand.registerAsResource(callback)
      }

      // try auto start
      serverToml <- Resource.eval(ServerToml.fromConfigFile[F])
      _ <- Resource.eval {
        serverToml match {
          case Left(e) => logger.warn(e)("failed to load config")
          case Right(toml) if toml.autoStart =>
            logger.info("autoStart = true") *> ModConfig.fromConfigFile
              .flatMap {
                case Left(e) => logger.error(e)("failed to load config")
                case Right(config) =>
                  runRealityLinkServer(
                    server,
                    supervisor,
                    events.broadcastingMessage,
                    config,
                  ).flatMap { runningServer =>
                    serverRef.set(Some(runningServer))
                  }
              }
          case Right(_) => ().pure[F]
        }
      }
    } yield ()
  }

  private def runRealityLinkServer[F[_]: {Async, LoggerFactory, Network}](
      server: MinecraftServer,
      supervisor: Supervisor[F],
      broadcastingMessage: CallbackBundle[F, Component, Unit],
      config: ModConfig,
  ): F[Fiber[F, Throwable, Nothing]] = {
    val logger = LoggerFactory[F].getLogger
    val interface: ChatInterface[F] =
      ChatInterface[F](server, broadcastingMessage, config.language)
    val runRealityLinkServerF: F[Nothing] = {
      val serverR =
        RealityLinkServer.run[F](config.serverConfig, interface, server)

      logger.info(
        show"Starting RealityLink server on ${config.serverConfig.host}:${config.serverConfig.port}"
      ) *> serverR.useForever
    }

    supervisor.supervise {
      MonadCancel[F].onCancel(
        runRealityLinkServerF,
        logger.info("RealityLink server stopped"),
      )
    }
  }

}
