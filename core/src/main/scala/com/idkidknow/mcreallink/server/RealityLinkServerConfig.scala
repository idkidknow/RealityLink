package com.idkidknow.mcreallink.server

import com.idkidknow.mcreallink.lib.platform.Language
import fs2.io.file.Path

final case class RealityLinkServerConfig(
  host: String,
  port: Int,
  tlsConfig: TlsConfig,
)

enum TlsConfig {
  case None
  case Tls(certChain: Path, privateKey: Path)
  case MutualTls(certChain: Path, privateKey: Path, root: Path)
}

final case class RealityLinkConfig[P[_]](
  port: Int,
  language: P[Language],
  tlsConfig: TlsConfig,
)
