package com.idkidknow.mcreallink

import com.akuleshov7.ktoml.Toml
import com.idkidknow.mcreallink.server.ApiServerConfig
import com.idkidknow.mcreallink.server.TlsConfig
import com.idkidknow.mcreallink.l10n.LanguageFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.nio.file.Files

private val logger = KotlinLogging.logger {}

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

data class ReadApiServerConfig<TLanguage>(val config: ApiServerConfig<TLanguage>, val autoStart: Boolean)
/** require [server] to get necessary information to start api server */
fun <TComponent, TLanguage, TMinecraftServer>
        ModContext<TComponent, TLanguage, TMinecraftServer>.readApiServerConfig(): Result<ReadApiServerConfig<TLanguage>> {

    val path = platform.configDir.resolve(MOD_ID).resolve("server.toml")
    val config = if (!Files.exists(path)) ServerToml() else
        Toml.decodeFromString(serializer(), Files.readAllBytes(path).toString(Charsets.UTF_8))

    val resPackDir = platform.gameDir.resolve(config.resourcePackDir)
    val languageFactory = LanguageFactory(platform)
    val fallbackLanguage = languageFactory.fromJavaResource(minecraftServer, config.localeCode)
    val resPackLanguage = languageFactory.fromResourcePackDir(resPackDir, config.localeCode).getOrElse {
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
            platform.configDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            platform.configDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
        )
    } else {
        TlsConfig.MutualTls(
            platform.configDir.resolve(MOD_ID).resolve(config.certChain).toFile(),
            platform.configDir.resolve(MOD_ID).resolve(config.privateKey).toFile(),
            platform.configDir.resolve(MOD_ID).resolve(config.root).toFile(),
        )
    }

    return Result.success(ReadApiServerConfig(
        config = ApiServerConfig(
            port = config.port,
            language = languageFactory.compose(resPackLanguage, fallbackLanguage),
            tlsConfig = tlsConfig,
        ),
        autoStart = config.autoStart,
    ))
}
