package com.idkidknow.mcrealcomm.fabric.platform

import com.idkidknow.mcrealcomm.platform.CommonEventManagers
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
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

    override fun createCommonEventManagers(): CommonEventManagers {
        return CommonEventManagers(Unit)
    }

    override fun getGameRootDir(): Path {
        return FabricLoader.getInstance().gameDir
    }

    override fun getGameConfigDir(): Path {
        return FabricLoader.getInstance().configDir
    }
}
