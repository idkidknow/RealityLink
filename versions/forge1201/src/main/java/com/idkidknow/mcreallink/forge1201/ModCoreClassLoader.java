package com.idkidknow.mcreallink.forge1201;

public class ModCoreClassLoader extends ClassLoader {
    private final ClassLoader parentClassLoader;

    ModCoreClassLoader(ClassLoader parent) {
        super(parent);
        parentClassLoader = parent;
    }
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = "/META-INF/mod-core/" + name.replace('.', '/') + ".class";
        try (var in = parentClassLoader.getResourceAsStream(path)) {
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
        return name.startsWith("io.netty");
    }
}
