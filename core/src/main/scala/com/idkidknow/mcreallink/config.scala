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
    val tlsConfig: F[TlsConfig] =
      (serverToml.certChain, serverToml.privateKey, serverToml.root) match {
        case (Some(certChain), Some(privateKey), None) =>
          TlsConfig
            .Tls(Path(certChain), Path(privateKey))
            .pure[F]
        case (Some(certChain), Some(privateKey), Some(root)) =>
          TlsConfig
            .MutualTls(Path(certChain), Path(privateKey), Path(root))
            .pure[F]
        case (None, None, None) =>
          TlsConfig.None.pure[F]
        case _ =>
          IllegalArgumentException("Invalid server.toml tls config").raiseError
      }
    val language: F[P[Language]] = {
      val fallback = LanguageMap.fromServer(server, serverToml.localeCode)
      val resourcePack = LanguageMap.fromResourcePackDirectory(Path(serverToml.resourcePackDir), serverToml.localeCode)
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
}
