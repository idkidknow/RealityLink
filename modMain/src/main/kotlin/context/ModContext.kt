package com.idkidknow.mcreallink.context

import com.idkidknow.mcreallink.platform.PlatformApi
import net.minecraft.server.MinecraftServer

interface ModContext : PlatformApi {
    val minecraftServer: MinecraftServer
    val modEvents: ModEvents
}
