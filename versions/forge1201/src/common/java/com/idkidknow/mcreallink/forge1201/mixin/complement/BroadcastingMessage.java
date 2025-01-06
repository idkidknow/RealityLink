package com.idkidknow.mcreallink.forge1201.mixin.complement;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class BroadcastingMessage {
    private static final ThreadLocal<Boolean> ignoreMessage = ThreadLocal.withInitial(() -> false);
    public static boolean isIgnoreMessage() {
        return ignoreMessage.get();
    }
    private static Consumer<Component> callback = component -> {};
    public static Consumer<Component> getCallback() {
        return callback;
    }
    public static void setCallback(Consumer<Component> callback) {
        BroadcastingMessage.callback = callback;
    }

    public static void ignoreTemporarily(Runnable action) {
        ignoreMessage.set(true);
        action.run();
        ignoreMessage.set(false);
    }
}
