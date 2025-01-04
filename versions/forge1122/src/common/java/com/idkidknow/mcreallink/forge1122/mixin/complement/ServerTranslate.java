package com.idkidknow.mcreallink.forge1122.mixin.complement;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.translation.LanguageMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @see LanguageMapMixin */
public class ServerTranslate {
    private static final ThreadLocal<LanguageMap> injectingLanguage = new ThreadLocal<>();

    public static @Nullable LanguageMap getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static @NotNull String translate(@NotNull ITextComponent text, @NotNull LanguageMap language) {
        injectingLanguage.set(language);
        String ret = text.getUnformattedText();
        injectingLanguage.remove();
        return ret;
    }
}
