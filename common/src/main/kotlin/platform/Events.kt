package com.idkidknow.mcrealcomm.platform

import com.idkidknow.mcrealcomm.event.EventManager
import net.minecraft.server.MinecraftServer

data class ServerStartingEvent(val server: MinecraftServer)
data class ServerStoppingEvent(val server: MinecraftServer)

data class ServerLifecycleEventManagers(
    val serverStarting: EventManager<ServerStartingEvent>,
    val serverStopping: EventManager<ServerStoppingEvent>,
)

data class CommonEventManagers(
    val unit: Unit
)
