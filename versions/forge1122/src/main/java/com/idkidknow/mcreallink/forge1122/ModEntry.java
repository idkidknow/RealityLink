package com.idkidknow.mcreallink.forge1122;

import com.idkidknow.mcreallink.forge1122.events.LifecycleEvents;
import com.idkidknow.mcreallink.forge1122.events.ModCommand;
import com.idkidknow.mcreallink.forge1122.events.Paths;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Method;

@Mod(modid = "reallink", version = "0.2.0-SNAPSHOT", name = "RealityLink", acceptableRemoteVersions = "*")
public class ModEntry {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Paths.configDirectory = event.getModConfigurationDirectory().toPath();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClassLoader parentClassLoader = ModEntry.class.getClassLoader();
        ClassLoader cl = new ModCoreClassLoader(parentClassLoader);
        try {
            Class<?> cls = cl.loadClass("com.idkidknow.mcreallink.forge1122.core.ScalaEntry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            LogManager.getLogger(ModEntry.class).error("Failed to load RealityLink", e);
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(ModCommand.getInstance());
        LifecycleEvents.getServerStartingCallback().accept(event.getServer());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        LifecycleEvents.getServerStoppingCallback().run();
    }
}
