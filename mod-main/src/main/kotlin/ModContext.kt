package com.idkidknow.mcreallink

import com.idkidknow.mcreallink.platform.Platform

class ModContext<TComponent, TLanguage, TMinecraftServer>(
    val platform: Platform<TComponent, TLanguage, TMinecraftServer>,
    val minecraftServer: TMinecraftServer,
)
