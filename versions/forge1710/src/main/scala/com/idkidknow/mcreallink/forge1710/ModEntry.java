package com.idkidknow.mcreallink.forge1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

@SuppressWarnings("unused")
@Mod(modid = "reallink", version = "0.2.0-alpha", name = "RealityLink", acceptableRemoteVersions = "*")
public class ModEntry {
    public static Path configDirectory = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDirectory = event.getModConfigurationDirectory().toPath();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ScalaEntry.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(ModCommand.getInstance());
        LifecycleEvents.serverStartingCallback().apply(event.getServer());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        LifecycleEvents.serverStoppingCallback().apply(MinecraftServer.getServer());
    }
}
