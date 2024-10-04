package com.idkidknow.mcrealcomm.api.server

import com.idkidknow.mcrealcomm.l10n.ServerLanguage
import java.io.File

data class ApiServerConfig(
    val port: Int,
    val language: ServerLanguage,
    val tlsConfig: TlsConfig,
)

sealed interface TlsConfig {
    object None : TlsConfig
    data class Tls(val certChain: File, val privateKey: File) : TlsConfig
    data class MutualTls(val certChain: File, val privateKey: File, val root: File) : TlsConfig
}
