package com.idkidknow.mcreallink

import cats.effect.Concurrent
import cats.effect.implicits.*
import cats.syntax.all.*
import com.idkidknow.mcreallink.utils.LanguageMap
import com.idkidknow.mcreallink.lib.platform.Language
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import com.idkidknow.mcreallink.lib.platform.Platform
import com.idkidknow.mcreallink.server.ApiServerConfig
import com.idkidknow.mcreallink.server.TlsConfig
import de.lhns.fs2.compress.Unarchiver
import fs2.io.file.Files
import fs2.io.file.Path
import org.typelevel.log4cats.Logger
import com.idkidknow.mcreallink.utils.decodeToml
import com.idkidknow.mcreallink.lib.ConfigReadingException
import cats.data.EitherT
import com.idkidknow.mcreallink.utils.ParseException
import java.io.IOException

final case class ServerToml(
    port: Int,
    localeCode: String,
    resourcePackDir: String,
    autoStart: Boolean,
    certChain: Option[String],
    privateKey: Option[String],
    root: Option[String],
)

final case class ModConfig[P[_]](
    apiServerConfig: ApiServerConfig[P],
    autoStart: Boolean,
)

object ModConfig {
  def fromServerToml[P[_], F[_]: Concurrent: Logger: Files](
      serverToml: ServerToml,
      server: P[MinecraftServer],
  )(using
      Platform[P, F],
      Unarchiver[F, Option, ?],
  ): F[Either[ConfigReadingException, ModConfig[P]]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]

    val gameRootDirectory = Platform[P, F].gameRootDirectory
    val configDirectory = Platform[P, F].configDirectory
    val certChain = serverToml.certChain.map(configDirectory.resolve)
    val privateKey = serverToml.privateKey.map(configDirectory.resolve)
    val root = serverToml.root.map(configDirectory.resolve(_))
    val resourcePackDir = gameRootDirectory.resolve(serverToml.resourcePackDir)

    val tlsConfig: FE[TlsConfig] =
      (certChain, privateKey, root) match {
        case (Some(certChain), Some(privateKey), None) =>
          TlsConfig
            .Tls(certChain, privateKey)
            .pure[FE]
        case (Some(certChain), Some(privateKey), Some(root)) =>
          TlsConfig
            .MutualTls(certChain, privateKey, root)
            .pure[FE]
        case (None, None, None) =>
          TlsConfig.None.pure[FE]
        case _ =>
          EitherT.leftT(
            ConfigReadingException.Format("Invalid server.toml tls config")
          )
      }
    val language: FE[P[Language]] = EitherT.right {
      val fallback = LanguageMap.fromServer(server, serverToml.localeCode)
      val resourcePack = LanguageMap.fromResourcePackDirectory(
        resourcePackDir,
        serverToml.localeCode,
      )
      (fallback, resourcePack).parMapN { (fallback, resourcePack) =>
        fallback.combine(resourcePack).toLanguage
      }
    }
    val apiServerConfig = (tlsConfig, language).mapN { (tlsConfig, language) =>
      ApiServerConfig(serverToml.port, language, tlsConfig)
    }
    apiServerConfig.map {
      ModConfig(_, serverToml.autoStart)
    }.value
  }

  def fromConfigFile[P[_], F[_]: Concurrent: Logger: Files](
      server: P[MinecraftServer]
  )(using
      Platform[P, F],
      Unarchiver[F, Option, ?],
  ): F[Either[ConfigReadingException, ModConfig[P]]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]

    val serverTomlPath = Platform[P, F].configDirectory / "server.toml"
    val serverTomlString: FE[String] = EitherT(
      Files[F]
        .readUtf8(serverTomlPath)
        .compile
        .string
        .attemptNarrow[IOException]
        .map { ioEither =>
          ioEither.leftMap(e => ConfigReadingException.IO(e))
        }
    )

    import io.circe.generic.auto.*
    serverTomlString.flatMap { str =>
      decodeToml[ServerToml](str) match {
        case Right(serverToml) =>
          EitherT(ModConfig.fromServerToml(serverToml, server))
        case Left(e) =>
          EitherT.leftT(ConfigReadingException.Parsing(e))
      }
    }.value
  }
}
