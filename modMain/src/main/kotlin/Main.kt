package com.idkidknow.mcrealcomm

import com.idkidknow.mcrealcomm.api.RegisteredCallback
import com.idkidknow.mcrealcomm.api.UnitCallbackSet
import com.idkidknow.mcrealcomm.api.invoke
import com.idkidknow.mcrealcomm.api.register
import com.idkidknow.mcrealcomm.server.ApiServer
import com.idkidknow.mcrealcomm.server.ApiServerConfig
import com.idkidknow.mcrealcomm.server.createApiServer
import com.idkidknow.mcrealcomm.command.modCommandBuilder
import com.idkidknow.mcrealcomm.context.CallingStartCommandEvent
import com.idkidknow.mcrealcomm.context.CallingStopCommandEvent
import com.idkidknow.mcrealcomm.context.ModContext
import com.idkidknow.mcrealcomm.context.ModEvents
import com.idkidknow.mcrealcomm.context.ModEventsInvoker
import com.idkidknow.mcrealcomm.platform.invoke
import com.idkidknow.mcrealcomm.l10n.LanguageLoadingException
import com.idkidknow.mcrealcomm.platform.Platform
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.ConcurrentSet
import net.minecraft.server.MinecraftServer
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

const val MOD_ID = "realcomm"

fun platformEntry(platform: Platform) {
    val modEvents = prepareModEvents(platform)
    modInit(platform, modEvents)
}

fun prepareModEvents(platform: Platform): ModEvents {
    val modEvents = ModEventsInvoker()
    platform.events {
        registerCommandsCallback { (dispatcher) ->
            val builder = modCommandBuilder(
                startAction = { modEvents.callingStartCommand.invokeFold(CallingStartCommandEvent, null) { e, invokeNext ->
                    if (e != null) return@invokeFold e
                    invokeNext()
                }},
                stopAction = { modEvents.callingStopCommand.invoke(CallingStopCommandEvent) },
            )
            dispatcher.register(builder)
        }
        broadcastingMessageCallback { event ->
            modEvents.broadcastingMessage.invoke(event)
        }
    }
    return modEvents
}

/** Initialize the mod and bind its lifetime to the Minecraft server */
fun modInit(platform: Platform, modEvents: ModEvents) {
    logger.info { "Reality Communication mod initializing" }

    val serverStarting = UnitCallbackSet<ServerStartingEvent>()
    val serverStopping = UnitCallbackSet<ServerStoppingEvent>()
    platform.events {
        serverStartingCallback { serverStarting.invoke(it) }
        serverStoppingCallback { serverStopping.invoke(it) }
    }

    serverStarting.addCallback { (server) ->
        val main = ModMain(object : PlatformApi by platform, ModContext {
            override val minecraftServer: MinecraftServer = server
            override val modEvents: ModEvents = modEvents
        })
        logger.info { "Minecraft server starting" }
        main.start()

        var registeredCallback: RegisteredCallback<ServerStoppingEvent, Unit>? = null
        registeredCallback = serverStopping.register {
            logger.info { "Minecraft server stopping" }
            main.stop()
            thread {
                registeredCallback?.unregister()
            }
        }
    }
}

class ModMain(val ctx: ModContext) : ModContext by ctx {
    private val logger = KotlinLogging.logger {}

    private val registeredCallbacks: MutableSet<RegisteredCallback<*, *>> = ConcurrentSet()
    private var apiServer: ApiServer? = null

    fun start() {
        modEvents.callingStartCommand.register { readConfigAndStartApiServer() }.let { registeredCallbacks.add(it) }
        modEvents.callingStopCommand.register { stopApiServer() }.let { registeredCallbacks.add(it) }

        readApiServerConfig(gameConfigDir, gameRootDir, minecraftServer).getOrNull()?.let {
            val (apiServerConfig, autoStart) = it
            if (autoStart) {
                logger.info { "autoStart = true" }
                startApiServer(apiServerConfig)
            }
        }
    }

    sealed class StartApiServerException(msg: String, cause: Exception? = null): Exception(msg, cause) {
        class AlreadyStarted: StartApiServerException("Already started")
        data class LanguageLoading(val e: LanguageLoadingException): StartApiServerException("Failed to load language", e)
    }
    private fun readConfigAndStartApiServer(): StartApiServerException? {
        val (apiServerConfig, _) = readApiServerConfig(gameConfigDir, gameRootDir, minecraftServer).getOrElse { e ->
            if (e is LanguageLoadingException) return StartApiServerException.LanguageLoading(e) else throw e
        }
        if (!startApiServer(apiServerConfig)) {
            return StartApiServerException.AlreadyStarted()
        }
        return null
    }

    /** @return false if already started */
    fun startApiServer(config: ApiServerConfig): Boolean {
        if (apiServer != null) return false
        logger.info { "Starting api server" }

        apiServer = createApiServer(config)
        apiServer!!.start()
        return true
    }

    fun stopApiServer() {
        apiServer?.stop()
        apiServer = null
    }

    fun stop() {
        stopApiServer()
        registeredCallbacks.forEach { it.unregister() }
        registeredCallbacks.clear()
    }
}
