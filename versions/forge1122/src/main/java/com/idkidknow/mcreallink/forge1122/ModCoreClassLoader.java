package com.idkidknow.mcreallink.forge1122;

import com.google.common.io.ByteStreams;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

public class ModCoreClassLoader extends ClassLoader {
    private final ClassLoader parentClassLoader;

    ModCoreClassLoader(ClassLoader parent) {
        super(parent);
        parentClassLoader = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = "META-INF/mod-core/" + name.replace('.', '/') + ".class";
        try (InputStream in = parentClassLoader.getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes = ByteStreams.toByteArray(in);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    protected URL findResource(String name) {
        return parentClassLoader.getResource("META-INF/mod-core/" + name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        URL url = findResource(name);
        if (url == null) return Collections.emptyEnumeration();
        return Collections.enumeration(Collections.singletonList(url));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (useMine(name)) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                try {
                    Class<?> c = findClass(name);
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                } catch (ClassNotFoundException ignored) {}
            }
        }

        synchronized (getClassLoadingLock(name)) {
            try {
                Class<?> c = parentClassLoader.loadClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException ignored) {}
        }

        return super.loadClass(name, resolve);
    }

    private boolean useMine(String name) {
        return name.startsWith("scala") || name.startsWith("io.netty");
    }
}
