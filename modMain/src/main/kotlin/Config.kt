package com.idkidknow.mcreallink

import com.akuleshov7.ktoml.Toml
import com.idkidknow.mcreallink.context.ModContext
import com.idkidknow.mcreallink.server.ApiServerConfig
import com.idkidknow.mcreallink.server.TlsConfig
import com.idkidknow.mcreallink.l10n.ServerLanguageFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.server.MinecraftServer
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

data class ReadApiServerConfig(val config: ApiServerConfig, val autoStart: Boolean)
fun ModContext.readApiServerConfig(): Result<ReadApiServerConfig> {
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

    val path = gameConfigDir.resolve(MOD_ID).resolve("server.toml")
    val config = if (!Files.exists(path)) ServerToml() else
        Toml.decodeFromString(serializer(), com.google.common.io.Files.asCharSource(path.toFile(), com.google.common.base.Charsets.UTF_8).read())

    val resPackDir = gameRootDir.resolve(config.resourcePackDir)
    val fallbackLanguage = with (ServerLanguageFactory) { fromJavaResource(config.localeCode) }
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
            gameConfigDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            gameConfigDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
        )
    } else {
        TlsConfig.MutualTls(
            gameConfigDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            gameConfigDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
            gameConfigDir.resolve(MOD_ID).resolve(config.root).toFile(),
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
