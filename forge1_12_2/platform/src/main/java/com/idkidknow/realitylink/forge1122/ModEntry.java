package com.idkidknow.realitylink.forge1122;

import net.minecraft.command.ICommand;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

@Mod(modid = "realitylink", version = Tag.VERSION, name = "RealityLink", acceptableRemoteVersions = "*")
public class ModEntry {
    private static final Logger logger = LogManager.getLogger(ModEntry.class);

    public static Path configDirectory;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDirectory = event.getModConfigurationDirectory().toPath();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClassLoader parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> isolatedClass = name -> name.startsWith("io.netty")
                || name.startsWith("scala")
                || name.startsWith("org.slf4j")
                || name.startsWith("org.apache.logging.slf4j");
        Function<String, Boolean> mcResourcesFirst = name -> name.startsWith("META-INF/services/")
                && !name.startsWith("META-INF/services/org.slf4j");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("realitylink.dev.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            try {
                Class<?> modLoadDevCls = Class.forName("com.idkidknow.realitylink.forge1122.ModLoadDev");
                Method developModeClassLoader = modLoadDevCls.getMethod(
                        "developModeClassLoader",
                        String.class,
                        ClassLoader.class,
                        Function.class,
                        Function.class
                );
                cl = (ClassLoader) developModeClassLoader.invoke(null, coreClasspathFile, parentClassLoader, isolatedClass, mcResourcesFirst);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            cl = ModLoad.productClassloader(parentClassLoader, isolatedClass, mcResourcesFirst);
        }
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> cls = cl.loadClass("com.idkidknow.realitylink.platform.Entry");
            Method method = cls.getMethod("entry");
            Thread.currentThread().setContextClassLoader(cl);
            method.invoke(null);
        } catch (Exception e) {
            Thread.currentThread().setContextClassLoader(original);
            logger.error("Failed to load RealityLink", e);
        }
    }

    public static ICommand command;
    public static Consumer<MinecraftServer> serverStartingCallback;
    public static Runnable serverStoppingCallback;

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(command);
        serverStartingCallback.accept(event.getServer());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        serverStoppingCallback.run();
    }
}
