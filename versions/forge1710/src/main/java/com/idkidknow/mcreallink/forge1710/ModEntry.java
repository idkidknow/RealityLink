package com.idkidknow.mcreallink.forge1710;

import com.idkidknow.mcreallink.forge1710.events.LifecycleEvents;
import com.idkidknow.mcreallink.forge1710.events.ModCommand;
import com.idkidknow.mcreallink.forge1710.events.Paths;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mod(modid = "reallink", version = "0.2.0-SNAPSHOT", name = "RealityLink", acceptableRemoteVersions = "*")
public class ModEntry {
    private static final Logger logger = LogManager.getLogger(ModEntry.class);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Paths.configDirectory = event.getModConfigurationDirectory().toPath();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClassLoader parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> useMine = name -> name.startsWith("scala")
                || name.startsWith("io.netty")
                || name.startsWith("org.slf4j");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("reallink.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            cl = ModLoad.developModeClassLoader(coreClasspathFile, parentClassLoader, useMine);
        } else {
            cl = ModLoad.productClassloader(parentClassLoader, useMine);
        }
        try {
            Class<?> cls = cl.loadClass("com.idkidknow.mcreallink.forge1710.core.ScalaEntry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to load RealityLink", e);
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
