package com.idkidknow.mcreallink.server

import java.io.File

data class ApiServerConfig<TLanguage>(
    val port: Int,
    val language: TLanguage,
    val tlsConfig: TlsConfig,
)

sealed interface TlsConfig {
    object None : TlsConfig
    data class Tls(val certChain: File, val privateKey: File) : TlsConfig
    data class MutualTls(val certChain: File, val privateKey: File, val root: File) : TlsConfig
}
