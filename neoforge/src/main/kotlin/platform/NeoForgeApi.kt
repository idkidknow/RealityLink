package com.idkidknow.mcrealcomm.neoforge.platform

import com.idkidknow.mcrealcomm.event.EventManager
import com.idkidknow.mcrealcomm.platform.CommonEventManagers
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Path

class NeoForgeApi: PlatformApi {
    override fun createServerLifecycleEventManagers(): ServerLifecycleEventManagers {
        val serverStarting: EventManager<ServerStartingEvent> =
            eventManagerFromNeoForge {handler ->
                { event: net.neoforged.neoforge.event.server.ServerStartingEvent ->
                    handler(ServerStartingEvent(event.server))
                }
            }
        val serverStopping: EventManager<ServerStoppingEvent> =
            eventManagerFromNeoForge {handler ->
                { event: net.neoforged.neoforge.event.server.ServerStoppingEvent ->
                    handler(ServerStoppingEvent(event.server))
                }
            }
        return ServerLifecycleEventManagers(serverStarting, serverStopping)
    }

    override fun createCommonEventManagers(): CommonEventManagers {
        return CommonEventManagers(Unit)
    }

    override fun getGameRootDir(): Path {
        return FMLPaths.GAMEDIR.get()
    }

    override fun getGameConfigDir(): Path {
        return FMLPaths.CONFIGDIR.get()
    }
}
