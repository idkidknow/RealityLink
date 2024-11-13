package com.idkidknow.mcreallink.server

import com.idkidknow.mcreallink.lib.platform.Language
import fs2.io.file.Path

final case class ApiServerConfig[P[_]](
  port: Int,
  language: P[Language],
  TlsConfig: TlsConfig,
)

enum TlsConfig {
  case None
  case Tls(certChain: Path, privateKey: Path)
  case MutualTls(certChain: Path, privateKey: Path, root: Path)
}
