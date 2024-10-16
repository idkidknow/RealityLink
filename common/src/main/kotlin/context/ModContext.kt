package com.idkidknow.mcrealcomm.context

import com.idkidknow.mcrealcomm.platform.PlatformApi
import net.minecraft.server.MinecraftServer

interface ModContext : PlatformApi {
    val minecraftServer: MinecraftServer
    val modEvents: ModEvents
}
