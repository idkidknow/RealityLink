package com.idkidknow.mcreallink.platform

import com.idkidknow.mcreallink.api.ComponentClass
import com.idkidknow.mcreallink.api.Events
import com.idkidknow.mcreallink.api.LanguageClass
import com.idkidknow.mcreallink.api.MinecraftServerClass
import java.nio.file.Path

interface Platform<TComponent, TLanguage, TMinecraftServer> {
    val instComponent: ComponentClass<TComponent, TLanguage, TMinecraftServer>
    val instLanguage: LanguageClass<TLanguage>
    val instMinecraftServer: MinecraftServerClass<TMinecraftServer, TComponent>
    val events: Events<TComponent, TMinecraftServer>
    val configDir: Path
    val gameDir: Path
    val minecraftClassLoader: ClassLoader
}
