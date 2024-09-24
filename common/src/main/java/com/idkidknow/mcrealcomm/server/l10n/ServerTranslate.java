package com.idkidknow.mcrealcomm.server.l10n;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.Nullable;

public class ServerTranslate {
    private static final ThreadLocal<Language> injectingLanguage = new ThreadLocal<>();

    public static @Nullable Language getInjectingLanguage() {
        return injectingLanguage.get();
    }

    public static String translate(FormattedText text, Language language) {
        injectingLanguage.set(language);
        var ret = text.getString();
        injectingLanguage.remove();
        return ret;
    }
}
