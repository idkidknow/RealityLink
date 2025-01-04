package com.idkidknow.mcreallink.forge1122.mixin.complement;

import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

public class BroadcastingMessage {
    private static final ThreadLocal<Boolean> ignoreMessage = ThreadLocal.withInitial(() -> false);
    public static boolean isIgnoreMessage() {
        return ignoreMessage.get();
    }
    private static Consumer<ITextComponent> callback = component -> {};
    public static Consumer<ITextComponent> getCallback() {
        return callback;
    }
    public static void setCallback(Consumer<ITextComponent> callback) {
        BroadcastingMessage.callback = callback;
    }

    public static void ignoreTemporarily(Runnable action) {
        ignoreMessage.set(true);
        action.run();
        ignoreMessage.set(false);
    }
}
