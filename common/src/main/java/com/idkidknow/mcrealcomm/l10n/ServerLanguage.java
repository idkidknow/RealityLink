package com.idkidknow.mcrealcomm.l10n;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ServerLanguage extends Language {
    public abstract Optional<@NotNull String> get(@NotNull String key);

    @Override
    public @NotNull String getOrDefault(@NotNull String key, @NotNull String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    @Override
    public boolean has(@NotNull String id) {
        return get(id).isPresent();
    }

    @Override
    public boolean isDefaultRightToLeft() {
        // only used in GUI and has no effects on server
        return false;
    }

    @Override
    public @NotNull FormattedCharSequence getVisualOrder(@NotNull FormattedText text) {
        // only used in GUI
        return FormattedCharSequence.EMPTY;
    }
}
