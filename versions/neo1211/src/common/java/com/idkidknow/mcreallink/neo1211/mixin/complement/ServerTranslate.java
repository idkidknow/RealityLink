package com.idkidknow.mcreallink.neo1211.mixin.complement;

import com.idkidknow.mcreallink.neo1211.mixin.mixin.LanguageMixin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @see LanguageMixin */
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
