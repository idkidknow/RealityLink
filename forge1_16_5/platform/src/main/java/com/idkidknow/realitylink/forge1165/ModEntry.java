package com.idkidknow.realitylink.forge1165;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mod("realitylink")
public class ModEntry {
    private static final Logger logger = LogManager.getLogger(ModEntry.class);

    public ModEntry() {
        ClassLoader parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> isolatedClass = name -> name.startsWith("scala")
                || name.startsWith("org.slf4j")
                || name.startsWith("org.apache.logging.slf4j");
        Function<String, Boolean> mcResourcesFirst = name -> name.startsWith("META-INF/services/")
                && !name.startsWith("META-INF/services/org.slf4j");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("realitylink.dev.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            try {
                Class<?> modLoadDevCls = Class.forName("com.idkidknow.realitylink.forge1165.ModLoadDev");
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
}
