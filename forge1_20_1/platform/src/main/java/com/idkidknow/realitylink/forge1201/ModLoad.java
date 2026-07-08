package com.idkidknow.realitylink.forge1201;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class ModLoad {
    private interface GetResources {
        Enumeration<URL> get(String name) throws IOException;
    }

    private static Enumeration<URL> findResourcesWith(String name, GetResources first, GetResources second) throws IOException {
        List<URL> urls = new ArrayList<>();
        try {
            var resources = first.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
        } catch (IOException ignored) {
            var resources = second.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            return Collections.enumeration(urls);
        }
        try {
            var resources = second.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            return Collections.enumeration(urls);
        } catch (IOException ignored) {
            return Collections.enumeration(urls);
        }
    }

    public static ClassLoader developModeClassLoader(
            String coreClasspathFile,
            ClassLoader mcClassLoader,
            Function<String, Boolean> isolatedClass,
            Function<String, Boolean> mcResourcesFirst
    ) {
        String coreClasspathStr;
        try {
            coreClasspathStr = Files.readString(Path.of(coreClasspathFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<URL> coreClasspath = Arrays.stream(coreClasspathStr.split("\n"))
                .map(str -> {
                    try {
                        return Paths.get(str).toUri().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        return new URLClassLoader(coreClasspath.toArray(new URL[0]), null) {
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
                    var url = mcClassLoader.getResource(name);
                    if (url != null) return url;
                }
                var url = super.findResource(name);
                if (url != null) return url;
                return mcClassLoader.getResource(name);
            }

            @Override
            public Enumeration<URL> findResources(String name) throws IOException {
                if (mcResourcesFirst.apply(name)) {
                    return findResourcesWith(name, mcClassLoader::getResources, super::findResources);
                } else {
                    return findResourcesWith(name, super::findResources, mcClassLoader::getResources);
                }
            }
        };
    }

    public static ClassLoader productClassloader(
            ClassLoader mcClassLoader,
            Function<String, Boolean> isolatedClass,
            Function<String, Boolean> mcResourcesFirst
    ) {
        return new ClassLoader(null) {
            private Class<?> findInnerClass(String name) throws ClassNotFoundException {
                String path = "META-INF/realitylink/" + name.replace('.', '/') + ".class";
                try (var in = mcClassLoader.getResourceAsStream(path)) {
                    if (in == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] bytes = in.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (isolatedClass.apply(name)) {
                    try {
                        return findInnerClass(name);
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                try {
                    return mcClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
                return findInnerClass(name);
            }

            @Override
            protected URL findResource(String name) {
                if (mcResourcesFirst.apply(name)) {
                    var url = mcClassLoader.getResource(name);
                    if (url != null) return url;
                }
                var url = mcClassLoader.getResource("META-INF/realitylink/" + name);
                if (url != null) return url;
                return mcClassLoader.getResource(name);
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                GetResources getCoreResources = n -> mcClassLoader.getResources("META-INF/realitylink/" + n);
                if (mcResourcesFirst.apply(name)) {
                    return findResourcesWith(name, mcClassLoader::getResources, getCoreResources);
                } else {
                    return findResourcesWith(name, getCoreResources, mcClassLoader::getResources);
                }
            }
        };
    }

}
