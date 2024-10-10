package com.idkidknow.mcrealcomm

import com.idkidknow.mcrealcomm.api.server.ApiServer
import com.idkidknow.mcrealcomm.api.server.ApiServerConfig
import com.idkidknow.mcrealcomm.api.server.createApiServer
import com.idkidknow.mcrealcomm.command.realcommCommandBuilder
import com.idkidknow.mcrealcomm.event.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.event.ModEvents
import com.idkidknow.mcrealcomm.event.SetEventManagerProxy
import com.idkidknow.mcrealcomm.event.SetUnitEventManagerProxy
import com.idkidknow.mcrealcomm.l10n.LanguageLoadingException
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import io.github.oshai.kotlinlogging.KotlinLogging
import net.minecraft.server.MinecraftServer
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

const val MOD_ID = "realcomm"

fun platformEntry(platformApi: PlatformApi) {
    modInit(platformApi, ModEvents())
}

/** Initialization all root events and bind ModContext's lifetime to the Minecraft server */
fun modInit(platformApi: PlatformApi, modEvents: ModEvents) {
    logger.info { "Reality Communication mod initializing" }

    val registerCommandsEvent = platformApi.createRegisterCommandsEventManager()
    registerCommandsEvent.addHandler { (dispatcher,_, _) ->
        val builder = realcommCommandBuilder(
            startAction = { modEvents.callingStartCommand(Unit) },
            stopAction = { modEvents.callingStopCommand(Unit) },
        )
        dispatcher.register(builder)
    }

    val lifecycleEvents = platformApi.createServerLifecycleEventManagers()
    lifecycleEvents.serverStarting.addHandler { (server) ->
        onServerStarting(platformApi, server, lifecycleEvents, modEvents)
    }
}

/** Manage the lifecycle of ModContext */
fun onServerStarting(
    api: PlatformApi,
    server: MinecraftServer,
    lifecycleEvents: ServerLifecycleEventManagers,
    modEvents: ModEvents,
) {
    logger.info { "Minecraft server starting" }

    val ctx = ModContext(
        platformApi = api,
        server = server,
        modEvents = modEvents,
    )

    lifecycleEvents.serverStopping.addHandler {_ ->
        // on server stopping
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
    modEvents: ModEvents,
) {
    private val logger = KotlinLogging.logger {}

    val gameDir get() = platformApi.getGameRootDir()
    val configDir get() = platformApi.getGameConfigDir()

    private val broadcastingMessage = SetUnitEventManagerProxy<BroadcastingMessageEvent>(modEvents.broadcastingMessage)
    private val callingStartCommand = SetEventManagerProxy<Unit, Exception?>(
        modEvents.callingStartCommand,
        defaultValue = null,
        foldFunction = { result, invokeNext -> result ?: invokeNext() },
    )
    private val callingStopCommand = SetUnitEventManagerProxy<Unit>(modEvents.callingStopCommand)

    private var apiServer: ApiServer? = null

    init {
        callingStartCommand.addHandler { readConfigAndStartApiServer() }
        callingStopCommand.addHandler { stopApiServer() }

        readApiServerConfig(configDir, gameDir, server).getOrNull()?.let {
            val (apiServerConfig, autoStart) = it
            if (autoStart) {
                logger.info { "autoStart = true" }
                startApiServer(apiServerConfig)
            }
        }
    }

    sealed class ApiServerStartingException(msg: String, cause: Exception? = null): Exception(msg, cause) {
        class AlreadyStarted: ApiServerStartingException("Already started")
        data class LanguageLoading(val e: LanguageLoadingException): ApiServerStartingException("Failed to load language", e)
    }
    private fun readConfigAndStartApiServer(): ApiServerStartingException? {
        val (apiServerConfig, _) = readApiServerConfig(configDir, gameDir, server).getOrElse { e ->
            if (e is LanguageLoadingException) return ApiServerStartingException.LanguageLoading(e) else throw e
        }
        if (!startApiServer(apiServerConfig)) {
            return ApiServerStartingException.AlreadyStarted()
        }
        return null
    }

    /** @return false if already started */
    fun startApiServer(config: ApiServerConfig): Boolean {
        if (apiServer != null) return false
        logger.info { "Starting api server" }

        apiServer = createApiServer(broadcastingMessage, config, server)
        apiServer!!.start()
        return true
    }

    fun stopApiServer() {
        apiServer?.stop()
        apiServer = null
    }

    fun stop() {
        stopApiServer()
        broadcastingMessage.removeProxy()
        callingStartCommand.removeProxy()
        callingStopCommand.removeProxy()
    }
}
