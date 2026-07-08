package com.idkidknow.realitylink.forge1122.mixin;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.translation.LanguageMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @see LanguageMapMixin
 */
public class ServerTranslate {
    private static final ThreadLocal<LanguageMap> injectingLanguage = new ThreadLocal<>();

    public static @Nullable LanguageMap getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static @Nonnull String translate(@Nonnull ITextComponent text, @Nonnull LanguageMap language) {
        injectingLanguage.set(language);
        String ret = text.getUnformattedText();
        injectingLanguage.remove();
        return ret;
    }
}
