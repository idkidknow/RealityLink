package com.idkidknow.mcreallink.forge1165;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModLoad {
    public static ClassLoader developModeClassLoader(
            String coreClasspathFile,
            ClassLoader mcClassLoader,
            Function<String, Boolean> useMine
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
        return new URLClassLoader(coreClasspath, null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (useMine.apply(name)) {
                    try {
                        return super.findClass(name);
                    } catch (ClassNotFoundException ignored) {}
                }
                try {
                    return mcClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {}
                return super.findClass(name);
            }

            @Override
            public URL findResource(String name) {
                URL url = mcClassLoader.getResource(name);
                if (url != null) return url;
                return super.findResource(name);
            }

            @Override
            public Enumeration<URL> findResources(String name) throws IOException {
                if (name.startsWith("META-INF/services/")) {
                    String interfaceName = name.substring("META-INF/services/".length());
                    if (useMine.apply(interfaceName)) {
                        return super.findResources(name);
                    }
                }
                try {
                    return mcClassLoader.getResources(name);
                } catch (IOException ignored) {}
                return super.findResources(name);
            }
        };
    }

    public static ClassLoader productClassloader(ClassLoader mcClassLoader, Function<String, Boolean> useMine) {
        return new ClassLoader(null) {
            private Class<?> findInnerClass(String name) throws ClassNotFoundException {
                String path = "META-INF/reallink-mod-core/" + name.replace('.', '/') + ".class";
                try (InputStream in = mcClassLoader.getResourceAsStream(path)) {
                    if (in == null) {
                        throw new ClassNotFoundException(name);
                    }
                    //noinspection UnstableApiUsage
                    byte[] bytes = ByteStreams.toByteArray(in);
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (useMine.apply(name)) {
                    try {
                        return findInnerClass(name);
                    } catch (ClassNotFoundException ignored) {}
                }
                try {
                    return mcClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {}
                return findInnerClass(name);
            }

            @Override
            protected URL findResource(String name) {
                URL url = mcClassLoader.getResource(name);
                if (url != null) return url;
                return mcClassLoader.getResource("META-INF/reallink-mod-core/" + name);
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                if (name.startsWith("META-INF/services/")) {
                    String interfaceName = name.substring("META-INF/services/".length());
                    if (useMine.apply(interfaceName)) {
                        return mcClassLoader.getResources("META-INF/reallink-mod-core/" + name);
                    }
                }
                try {
                    return mcClassLoader.getResources(name);
                } catch (IOException ignored) {}
                return mcClassLoader.getResources("META-INF/reallink-mod-core/" + name);
            }
        };
    }

}
