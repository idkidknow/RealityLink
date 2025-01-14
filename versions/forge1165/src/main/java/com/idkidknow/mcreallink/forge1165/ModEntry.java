package com.idkidknow.mcreallink.forge1165;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mod("reallink")
public class ModEntry {
    private static final Logger logger = LogManager.getLogger(ModEntry.class);

    public ModEntry() {
        ClassLoader parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> useMine = name -> name.startsWith("scala")
                || name.startsWith("io.netty")
                || name.startsWith("org.slf4j")
                || name.startsWith("org.apache.logging.slf4j");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("reallink.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            cl = ModLoad.developModeClassLoader(coreClasspathFile, parentClassLoader, useMine);
        } else {
            cl = ModLoad.productClassloader(parentClassLoader, useMine);
        }
        try {
            Class<?> cls = cl.loadClass("com.idkidknow.mcreallink.forge1165.core.ScalaEntry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to load RealityLink", e);
        }
    }
}
