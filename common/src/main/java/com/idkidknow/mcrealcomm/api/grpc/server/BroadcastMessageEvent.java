package com.idkidknow.mcrealcomm.api.grpc.server;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BroadcastMessageEvent {
    private static final Set<Consumer<Component>> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static void register(Consumer<Component> listener) {
        listeners.add(listener);
    }
    public static void unregister(Consumer<Component> listener) {
        listeners.remove(listener);
    }
    public static void invoke(Component message) {
        listeners.forEach(listener -> listener.accept(message));
    }
    public static void clear() {
        listeners.clear();
    }
}
