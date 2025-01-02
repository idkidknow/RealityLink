package com.idkidknow.mcreallink.forge1182;

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
}
