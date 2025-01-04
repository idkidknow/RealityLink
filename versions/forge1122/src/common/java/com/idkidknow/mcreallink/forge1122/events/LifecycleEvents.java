package com.idkidknow.mcreallink.forge1122.events;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public class LifecycleEvents {
    private static Consumer<MinecraftServer> serverStartingCallback;
    private static Runnable serverStoppingCallback;

    public static Consumer<MinecraftServer> getServerStartingCallback() {
        return serverStartingCallback;
    }

    public static Runnable getServerStoppingCallback() {
        return serverStoppingCallback;
    }

    public static void setServerStartingCallback(Consumer<MinecraftServer> serverStartingCallback) {
        LifecycleEvents.serverStartingCallback = serverStartingCallback;
    }

    public static void setServerStoppingCallback(Runnable serverStoppingCallback) {
        LifecycleEvents.serverStoppingCallback = serverStoppingCallback;
    }
}
