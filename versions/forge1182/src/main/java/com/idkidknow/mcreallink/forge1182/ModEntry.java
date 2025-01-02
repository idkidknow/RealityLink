package com.idkidknow.mcreallink.forge1182;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@Mod("reallink")
public class ModEntry {
    public ModEntry() {
        var parentClassLoader = ModEntry.class.getClassLoader();
        var cl = new ModCoreClassLoader(parentClassLoader);
        try {
            var cls = cl.loadClass("com.idkidknow.mcreallink.forge1182.core.ScalaEntry");
            Method method = cls.getMethod("entry");
            method.invoke(null);
        } catch (Exception e) {
            LoggerFactory.getLogger(ModEntry.class).error("Failed to load RealityLink", e);
        }
    }
}
