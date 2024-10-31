package com.idkidknow.mcreallink.platform.neoforge121.mixin.complement;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @see com.idkidknow.mcreallink.platform.neoforge121.mixin.mixin.LanguageMixin */
public class ServerTranslate {
    private static final ThreadLocal<Language> injectingLanguage = new ThreadLocal<>();

    public static @Nullable Language getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static @NotNull String translate(@NotNull FormattedText text, @NotNull Language language) {
        injectingLanguage.set(language);
        var ret = text.getString();
        injectingLanguage.remove();
        return ret;
    }
}
