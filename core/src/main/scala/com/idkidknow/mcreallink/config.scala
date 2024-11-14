package com.idkidknow.mcrealcomm

import cats.effect.Concurrent
import cats.effect.implicits.*
import cats.syntax.all.*
import com.idkidknow.mcrealcomm.utils.LanguageMap
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
  ): F[ModConfig[P]] = {
    val gameRootDirectory = Platform[P, F].gameRootDirectory
    val configDirectory = Platform[P, F].configDirectory
    val certChain = serverToml.certChain.map(configDirectory.resolve(_))
    val privateKey = serverToml.privateKey.map(configDirectory.resolve(_))
    val root = serverToml.root.map(configDirectory.resolve(_))
    val resourcePackDir = gameRootDirectory.resolve(serverToml.resourcePackDir)

    val tlsConfig: F[TlsConfig] =
      (certChain, privateKey, root) match {
        case (Some(certChain), Some(privateKey), None) =>
          TlsConfig
            .Tls(certChain, privateKey)
            .pure[F]
        case (Some(certChain), Some(privateKey), Some(root)) =>
          TlsConfig
            .MutualTls(certChain, privateKey, root)
            .pure[F]
        case (None, None, None) =>
          TlsConfig.None.pure[F]
        case _ =>
          IllegalArgumentException("Invalid server.toml tls config").raiseError
      }
    val language: F[P[Language]] = {
      val fallback = LanguageMap.fromServer(server, serverToml.localeCode)
      val resourcePack = LanguageMap.fromResourcePackDirectory(resourcePackDir, serverToml.localeCode)
      (fallback, resourcePack).parMapN { (fallback, resourcePack) =>
        fallback.combine(resourcePack).toLanguage
      }
    }
    val apiServerConfig = (tlsConfig, language).mapN { (tlsConfig, language) =>
      ApiServerConfig(serverToml.port, language, tlsConfig)
    }
    apiServerConfig.map {
      ModConfig(_, serverToml.autoStart)
    }
  }

  def fromConfigFile[P[_], F[_]: Concurrent: Logger: Files](
    server: P[MinecraftServer],
  )(using
    Platform[P, F],
    Unarchiver[F, Option, ?],
  ): F[ModConfig[P]] = {
    val serverTomlPath = Platform[P, F].configDirectory / "server.toml"
    val serverTomlString = Files[F].readUtf8(serverTomlPath).compile.string
    import io.circe.generic.auto.*
    serverTomlString.flatMap { str =>
      decodeToml[ServerToml](str) match {
        case Right(serverToml) =>
          ModConfig.fromServerToml(serverToml, server)
        case Left(e) => e.raiseError
      }
    }
  }
}
