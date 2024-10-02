package com.idkidknow.mcrealcomm.fabric.platform

import com.idkidknow.mcrealcomm.event.UnitEventManager
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.RegisterCommandsEvent
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

class FabricApi: PlatformApi {
    override fun createServerLifecycleEventManagers(): ServerLifecycleEventManagers {
        val serverStarting =
            eventManagerFromFabric(ServerLifecycleEvents.SERVER_STARTING) { handler ->
                ServerLifecycleEvents.ServerStarting { handler(ServerStartingEvent(it)) }
            }
        val serverStopping =
            eventManagerFromFabric(ServerLifecycleEvents.SERVER_STOPPING) { handler ->
                ServerLifecycleEvents.ServerStopping { handler(ServerStoppingEvent(it)) }
            }
        return ServerLifecycleEventManagers(serverStarting, serverStopping)
    }

    override fun createRegisterCommandsEventManager(): UnitEventManager<RegisterCommandsEvent> =
        eventManagerFromFabric(CommandRegistrationCallback.EVENT) { handler ->
            CommandRegistrationCallback { dispatcher, context, environment ->
                handler(RegisterCommandsEvent(dispatcher, environment, context))
            }
    }

    override fun getGameRootDir(): Path {
        return FabricLoader.getInstance().gameDir
    }

    override fun getGameConfigDir(): Path {
        return FabricLoader.getInstance().configDir
    }
}
