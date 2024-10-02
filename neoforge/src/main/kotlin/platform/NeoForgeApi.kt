package com.idkidknow.mcrealcomm.neoforge.platform

import com.idkidknow.mcrealcomm.event.UnitEventManager
import com.idkidknow.mcrealcomm.platform.PlatformApi
import com.idkidknow.mcrealcomm.platform.RegisterCommandsEvent
import com.idkidknow.mcrealcomm.platform.ServerLifecycleEventManagers
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Path

class NeoForgeApi: PlatformApi {
    override fun createServerLifecycleEventManagers(): ServerLifecycleEventManagers {
        val serverStarting: UnitEventManager<ServerStartingEvent> =
            eventManagerFromNeoForge { handler ->
                { event: net.neoforged.neoforge.event.server.ServerStartingEvent ->
                    handler(ServerStartingEvent(event.server))
                }
            }
        val serverStopping: UnitEventManager<ServerStoppingEvent> =
            eventManagerFromNeoForge { handler ->
                { event: net.neoforged.neoforge.event.server.ServerStoppingEvent ->
                    handler(ServerStoppingEvent(event.server))
                }
            }
        return ServerLifecycleEventManagers(serverStarting, serverStopping)
    }

    override fun createRegisterCommandsEventManager(): UnitEventManager<RegisterCommandsEvent> = eventManagerFromNeoForge { handler ->
        { event : net.neoforged.neoforge.event.RegisterCommandsEvent ->
            handler(RegisterCommandsEvent(event.dispatcher, event.commandSelection, event.buildContext))
        }
    }

    override fun getGameRootDir(): Path {
        return FMLPaths.GAMEDIR.get()
    }

    override fun getGameConfigDir(): Path {
        return FMLPaths.CONFIGDIR.get()
    }
}
