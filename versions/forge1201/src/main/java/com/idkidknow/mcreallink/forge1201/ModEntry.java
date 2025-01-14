package com.idkidknow.mcreallink.forge1201;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mod("reallink")
public class ModEntry {
    private static final Logger logger = LoggerFactory.getLogger(ModEntry.class);

    public ModEntry() {
        var parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> useMine = name -> name.startsWith("io.netty");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("reallink.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            cl = ModLoad.developModeClassLoader(coreClasspathFile, parentClassLoader, useMine);
        } else {
            cl = ModLoad.productClassloader(parentClassLoader, useMine);
        }
        try {
            var cls = cl.loadClass("com.idkidknow.mcreallink.forge1201.core.ScalaEntry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to load RealityLink", e);
        }
    }
}
