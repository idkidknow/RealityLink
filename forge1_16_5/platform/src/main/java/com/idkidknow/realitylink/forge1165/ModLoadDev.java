package com.idkidknow.realitylink.forge1165;

import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.classloader.DowngradingClassLoader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.Function;

import static com.idkidknow.realitylink.forge1165.ModLoad.findResourcesWith;

public class ModLoadDev {
    public static ClassLoader developModeClassLoader(
            String coreClasspathFile,
            ClassLoader mcClassLoader,
            Function<String, Boolean> isolatedClass,
            Function<String, Boolean> mcResourcesFirst
    ) {
        String coreClasspathStr;
        try {
            coreClasspathStr = new String(Files.readAllBytes(Paths.get(coreClasspathFile)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        URL[] coreClasspath = Arrays.stream(coreClasspathStr.split("\n"))
                .map(str -> {
                    try {
                        return Paths.get(str).toUri().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        DowngradingClassLoader loader;
        try {
            loader = new DowngradingClassLoader(ClassDowngrader.getCurrentVersionDowngrader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if (isolatedClass.apply(name)) {
                        try {
                            return super.findClass(name);
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    try {
                        return mcClassLoader.loadClass(name);
                    } catch (ClassNotFoundException ignored) {
                        return super.findClass(name);
                    }
                }

                @Override
                public URL findResource(String name) {
                    if (mcResourcesFirst.apply(name)) {
                        URL url = mcClassLoader.getResource(name);
                        if (url != null) return url;
                    }
                    URL url = super.findResource(name);
                    if (url != null) return url;
                    return mcClassLoader.getResource(name);
                }

                @Override
                public Enumeration<URL> findResources(String name) {
                    try {
                        if (mcResourcesFirst.apply(name)) {
                            return findResourcesWith(name, mcClassLoader::getResources, super::findResources);
                        } else {
                            return findResourcesWith(name, super::findResources, mcClassLoader::getResources);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loader.addDelegate(new URLClassLoader(coreClasspath, null));
        return loader;
    }
}
