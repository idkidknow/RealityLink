package com.idkidknow.mcreallink

import com.idkidknow.mcreallink.api.RegisteredCallback
import com.idkidknow.mcreallink.api.ServerStoppingEvent
import com.idkidknow.mcreallink.api.register
import com.idkidknow.mcreallink.server.ApiServer
import com.idkidknow.mcreallink.server.ApiServerConfig
import com.idkidknow.mcreallink.server.createApiServer
import com.idkidknow.mcreallink.platform.Platform
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.ConcurrentSet
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

const val MOD_ID = "reallink"

/** Initialize the mod and bind its lifetime to the Minecraft server */
fun <TComponent, TLanguage, TMinecraftServer> modInit(platform: Platform<TComponent, TLanguage, TMinecraftServer>) {
    logger.info { "RealityLink mod initializing" }
    platform.events.serverStarting.addCallback { (server) ->
        val main = ModMain(platform, server)
        logger.info { "Minecraft server starting" }
        main.start()

        // stop ModMain when server stopping
        var registeredCallback: RegisteredCallback<ServerStoppingEvent<TMinecraftServer>, Unit>? = null
        registeredCallback = platform.events.serverStopping.register {
            logger.info { "Minecraft server stopping" }
            main.stop()
            // avoid iterator invalidation
            thread {
                registeredCallback?.unregister()
            }
        }
    }
}

class ModMain<TComponent, TLanguage, TMinecraftServer>(
    platform: Platform<TComponent, TLanguage, TMinecraftServer>,
    server: TMinecraftServer
) : Platform<TComponent, TLanguage, TMinecraftServer> by platform {
    private val logger = KotlinLogging.logger {}

    private val ctx = ModContext(platform, server)
    private val registeredCallbacks: MutableSet<RegisteredCallback<*, *>> = ConcurrentSet()
    private var apiServer: ApiServer? = null

    fun start() {
        events.callingStartCommand.register {
            val (config, _) = ctx.readApiServerConfig().getOrElse { e ->
                return@register Result.failure(e)
            }
            val ret = startApiServer(config)
            if (ret == false) {
                return@register Result.failure(Exception("API server already started"))
            }
            return@register Result.success(Unit)
        }.let { registeredCallbacks.add(it) }
        events.callingStopCommand.register { stopApiServer() }.let { registeredCallbacks.remove(it) }

        val (config, autoStart) = ctx.readApiServerConfig().getOrThrow()
        if (autoStart) {
            logger.info { "autoStart = true" }
            startApiServer(config)
        }
    }

    /** @return false if already started */
    fun startApiServer(config: ApiServerConfig<TLanguage>): Boolean {
        if (apiServer != null) return false
        logger.info { "Starting api server" }

        apiServer = ctx.createApiServer(config)
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
