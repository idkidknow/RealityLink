package com.idkidknow.realitylink.forge1710;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

public class ModLoad {
    public interface GetResources {
        Enumeration<URL> get(String name) throws IOException;
    }

    public static Enumeration<URL> findResourcesWith(String name, GetResources first, GetResources second) throws IOException {
        List<URL> urls = new ArrayList<>();
        try {
            Enumeration<URL> resources = first.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
        } catch (IOException ignored) {
            Enumeration<URL> resources = second.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            return Collections.enumeration(urls);
        }
        try {
            Enumeration<URL> resources = second.get(name);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
            return Collections.enumeration(urls);
        } catch (IOException ignored) {
            return Collections.enumeration(urls);
        }
    }

    public static ClassLoader productClassloader(
            ClassLoader mcClassLoader,
            Function<String, Boolean> isolatedClass,
            Function<String, Boolean> mcResourcesFirst
    ) {
        return new ClassLoader(null) {
            private Class<?> findInnerClass(String name) throws ClassNotFoundException {
                String path = "META-INF/realitylink/" + name.replace('.', '/') + ".class";
                try (InputStream in = mcClassLoader.getResourceAsStream(path)) {
                    if (in == null) {
                        throw new ClassNotFoundException(name);
                    }
                    @SuppressWarnings("UnstableApiUsage") byte[] bytes = ByteStreams.toByteArray(in);
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
                    URL url = mcClassLoader.getResource(name);
                    if (url != null) return url;
                }
                URL url = mcClassLoader.getResource("META-INF/realitylink/" + name);
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
