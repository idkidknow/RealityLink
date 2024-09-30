package com.idkidknow.mcrealcomm.api.server

import com.idkidknow.mcrealcomm.l10n.ServerLanguage

data class ApiServerConfig(
    val port: Int,
    val language: ServerLanguage,
)