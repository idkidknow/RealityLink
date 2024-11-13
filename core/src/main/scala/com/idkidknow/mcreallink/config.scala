package com.idkidknow.mcrealcomm

import cats.MonadThrow
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.platform.Platform
import com.idkidknow.mcreallink.server.ApiServerConfig
import com.idkidknow.mcreallink.server.TlsConfig
import fs2.io.file.Path

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
  def fromServerToml[P[_], F[_]: MonadThrow](
      serverToml: ServerToml,
  )(using Platform[P, F]): F[ModConfig[P]] = {
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
    ???
  }
}
