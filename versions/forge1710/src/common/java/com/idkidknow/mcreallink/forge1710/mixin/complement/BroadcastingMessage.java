package com.idkidknow.mcreallink.forge1710.mixin.complement;

import net.minecraft.util.IChatComponent;

import java.util.function.Consumer;

public class BroadcastingMessage {
    private static final ThreadLocal<Boolean> ignoreMessage = ThreadLocal.withInitial(() -> false);
    public static boolean isIgnoreMessage() {
        return ignoreMessage.get();
    }
    private static Consumer<IChatComponent> callback = component -> {};
    public static Consumer<IChatComponent> getCallback() {
        return callback;
    }
    public static void setCallback(Consumer<IChatComponent> callback) {
        BroadcastingMessage.callback = callback;
    }

    public static void ignoreTemporarily(Runnable action) {
        ignoreMessage.set(true);
        action.run();
        ignoreMessage.set(false);
    }
}
