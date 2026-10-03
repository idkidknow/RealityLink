package com.idkidknow.realitylink.forge1122.mixin;

import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public class ServerTranslate {
    private static final ThreadLocal<Function<String, Optional<String>>> injectingLanguage = new ThreadLocal<>();

    public static @Nullable Function<String, Optional<String>> getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static @Nonnull String translate(@Nonnull ITextComponent text, @Nonnull Function<String, Optional<String>> language) {
        injectingLanguage.set(language);
        try {
            return text.getUnformattedText();
        } finally {
            injectingLanguage.remove();
        }
    }
}
