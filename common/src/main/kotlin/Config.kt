package com.idkidknow.mcrealcomm

import com.akuleshov7.ktoml.Toml
import com.idkidknow.mcrealcomm.api.server.ApiServerConfig
import com.idkidknow.mcrealcomm.api.server.TlsConfig
import com.idkidknow.mcrealcomm.l10n.ServerLanguageFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.server.MinecraftServer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class ReadApiServerConfig(val config: ApiServerConfig, val autoStart: Boolean)
/** require [server] to get necessary information to start api server */
fun readApiServerConfig(
    configDir: Path,
    gameDir: Path,
    server: MinecraftServer,
): Result<ReadApiServerConfig> {
    @Serializable
    data class ServerToml(
        val port: Int = 39244,
        val localeCode: String = "en_us",
        val resourcePackDir: String = "serverlang",
        val autoStart: Boolean = false,
        val certChain: String? = null,
        val privateKey: String? = null,
        val root: String? = null,
    )

    val path = configDir.resolve(MOD_ID).resolve("server.toml")
    val config = if (!Files.exists(path)) ServerToml() else Toml.decodeFromString(serializer(), Files.readString(path))

    val resPackDir = gameDir.resolve(config.resourcePackDir)
    val fallbackLanguage = ServerLanguageFactory.fromJavaResource(server, config.localeCode)
    val resPackLanguage = ServerLanguageFactory.fromResourcePackDir(resPackDir, config.localeCode).getOrElse {
        logger.error(it) {}
        return Result.failure(it)
    }

    val tlsConfig = if (config.certChain == null) {
        TlsConfig.None
    } else if (config.privateKey == null) {
        logger.error { "missing private key" }
        throw IllegalArgumentException("missing private key")
    } else if (config.root == null) {
        TlsConfig.Tls(
            configDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            configDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
        )
    } else {
        TlsConfig.MutualTls(
            configDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            configDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
            configDir.resolve(MOD_ID).resolve(config.root).toFile(),
        )
    }

    return Result.success(ReadApiServerConfig(
        config = ApiServerConfig(
            port = config.port,
            language = ServerLanguageFactory.compose(resPackLanguage, fallbackLanguage),
            tlsConfig = tlsConfig,
        ),
        autoStart = config.autoStart,
    ))
}
