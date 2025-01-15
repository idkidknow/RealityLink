package com.idkidknow.mcreallink.forge1710.mixin.complement;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringTranslate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class ServerTranslate {
    private static final ThreadLocal<Function<String, Optional<String>>> injectingLanguage = new ThreadLocal<>();

    public static @Nullable Function<String, Optional<String>> getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static @NotNull String translate(@NotNull IChatComponent component, @NotNull Function<String, Optional<String>> language) {
        IChatComponent copied = component.createCopy();
        injectingLanguage.set(language);
        String ret = copied.getUnformattedTextForChat();
        injectingLanguage.remove();
        return ret;
    }
}
