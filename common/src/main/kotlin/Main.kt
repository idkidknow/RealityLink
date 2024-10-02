package com.idkidknow.mcrealcomm

import com.akuleshov7.ktoml.Toml
import com.idkidknow.mcrealcomm.api.server.ApiServer
import com.idkidknow.mcrealcomm.api.server.ApiServerConfig
import com.idkidknow.mcrealcomm.event.BroadCastingMessageEventManager
import com.idkidknow.mcrealcomm.event.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.event.EventManager
import com.idkidknow.mcrealcomm.event.EventManagerProxy
import com.idkidknow.mcrealcomm.l10n.ServerLanguageFactory
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.server.MinecraftServer
import java.nio.file.Files
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

const val MOD_ID = "realcomm"

fun modInit(platformApi: PlatformApi) {
    logger.info { "Reality Communication mod initializing" }
    val lifecycleEvents = platformApi.createServerLifecycleEventManagers()
    lifecycleEvents.serverStarting.addHandler { (server) ->
        onServerStarting(platformApi, server, lifecycleEvents)
    }
}

/** Manage the lifecycle of ModContext */
fun onServerStarting(api: PlatformApi, server: MinecraftServer, lifecycleEvents: ServerLifecycleEventManagers) {
    logger.info { "Minecraft server starting" }

    val ctx = ModContext(
        platformApi = api,
        server = server,
        broadcastingMessageEventManager = BroadCastingMessageEventManager,
    )

    lifecycleEvents.serverStopping.addHandler {_ ->
        logger.info { "Minecraft server stopping" }
        ctx.stop()
        thread {
            lifecycleEvents.serverStopping.clear()
        }
    }
}

class ModContext(
    val platformApi: PlatformApi,
    val server: MinecraftServer,
    broadcastingMessageEventManager: EventManager<BroadcastingMessageEvent>,
) {
    private val logger = KotlinLogging.logger {}

    val gameDir get() = platformApi.getGameRootDir()
    val configDir get() = platformApi.getGameConfigDir()

    private val broadcastingMessage = EventManagerProxy<BroadcastingMessageEvent>(broadcastingMessageEventManager)
    private var apiServer: ApiServer? = null

    init {
        readApiServerConfig().getOrNull()?.let {
            val (apiServerConfig, autoStart) = it
            if (autoStart) {
                logger.info { "autoStart = true" }
                startApiServer(apiServerConfig)
            }
        }
    }

    /** @return `false` if a server has already started */
    fun startApiServer(config: ApiServerConfig): Boolean {
        if (apiServer != null) return false
        logger.info { "Starting api server" }
        apiServer = ApiServer(broadcastingMessage, config, server)
        return true
    }

    fun stopApiServer() {
        apiServer?.stop()
    }

    fun stop() {
        stopApiServer()
        broadcastingMessage.removeProxy()
    }

    data class ReadApiServerConfig(val config: ApiServerConfig, val autoStart: Boolean)
    fun readApiServerConfig(): Result<ReadApiServerConfig> {
        @Serializable
        data class ServerToml(
            val port: Int = 39244,
            val localeCode: String = "en_us",
            val resourcePackDir: String = "serverlang",
            val autoStart: Boolean = false,
        )

        val path = configDir.resolve(MOD_ID).resolve("server.toml")
        val config = if (!Files.exists(path)) ServerToml() else Toml.decodeFromString(serializer(), Files.readString(path))

        val resPackDir = gameDir.resolve(config.resourcePackDir)
        val fallbackLanguage = ServerLanguageFactory.fromJavaResource(server, config.localeCode)
        val resPackLanguage = ServerLanguageFactory.fromResourcePackDir(resPackDir, config.localeCode).getOrElse {
            logger.error(it) {}
            return Result.failure(it)
        }
        return Result.success(ReadApiServerConfig(
            config = ApiServerConfig(
                port = config.port,
                language = ServerLanguageFactory.compose(resPackLanguage, fallbackLanguage),
            ),
            autoStart = config.autoStart,
        ))
    }
}
