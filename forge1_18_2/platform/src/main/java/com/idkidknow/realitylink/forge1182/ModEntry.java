package com.idkidknow.realitylink.forge1182;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mod("realitylink")
public class ModEntry {
    private static final Logger logger = LoggerFactory.getLogger(ModEntry.class);

    public ModEntry() {
        var parentClassLoader = ModEntry.class.getClassLoader();
        Function<String, Boolean> isolatedClass = name -> name.startsWith("io.netty");
        Function<String, Boolean> mcResourcesFirst = name -> name.startsWith("META-INF/services/");
        ClassLoader cl;
        String coreClasspathFile = System.getProperty("realitylink.dev.core.classpath");
        if (coreClasspathFile != null) {
            logger.info("Loading RealityLink in dev mode");
            cl = ModLoad.developModeClassLoader(coreClasspathFile, parentClassLoader, isolatedClass, mcResourcesFirst);
        } else {
            cl = ModLoad.productClassloader(parentClassLoader, isolatedClass, mcResourcesFirst);
        }
        try {
            var cls = cl.loadClass("com.idkidknow.realitylink.platform.Entry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to load RealityLink", e);
        }
    }
}
