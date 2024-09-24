package com.idkidknow.mcrealcomm.server.l10n;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ServerLanguage extends Language {
    public abstract Optional<String> get(@Nonnull String key);

    @Override
    public @Nonnull String getOrDefault(@Nonnull String key, @Nonnull String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    @Override
    public boolean has(@Nonnull String id) {
        return get(id).isPresent();
    }

    @Override
    public boolean isDefaultRightToLeft() {
        // only used in GUI and has no effects on server
        return false;
    }

    @Override
    public @Nonnull FormattedCharSequence getVisualOrder(@Nonnull FormattedText text) {
        // only used in GUI
        return FormattedCharSequence.EMPTY;
    }

    private static final Logger logger = LogUtils.getLogger();

    /**
     * Read language files in Java resources
     * Under normal circumstances, there are Minecraft's en_us.json and all language json in every mods' jar file
     * */
    public static @Nonnull ServerLanguage getResourceLanguage(Iterable<String> namespaces, String localeCode) {
        var builder = ImmutableMap.<String, String>builder();
        for (var namespace : namespaces) {
            var path = String.format("/assets/%s/lang/%s.json", namespace, localeCode);
            try (var inputStream = Language.class.getResourceAsStream(path)) {
                if (inputStream == null) {
                    logger.info("{} not in resources", path);
                    continue;
                }
                Language.loadFromJson(inputStream, builder::put);
                logger.info("{} loaded", path);
            } catch (Throwable e) {
                logger.warn("Failed to load {}: ", path, e);
            }
        }
        var map = builder.build();
        return new ServerLanguage() {
            @Override
            public Optional<String> get(@NotNull String key) {
                return Optional.ofNullable(map.get(key));
            }
        };
    }

    public static @Nonnull ServerLanguage getResourceLanguage(MinecraftServer server, String localeCode) {
        return getResourceLanguage(server.getResourceManager().getNamespaces(), localeCode);
    }

    /**
     * Read language files in resources packs of given paths
     * */
    public static @Nonnull ServerLanguage getResourcePackLanguage(Iterable<Path> paths, String localeCode) {
        var builder = ImmutableMap.<String, String>builder();
        for (var path : paths) {
            logger.info("loading resource pack {}", path);
            try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                loadFromResourcePack(input, localeCode, builder::put);
            } catch (IOException e) {
                logger.warn("Failed to load resource pack {}: ", path, e);
            }
        }
        var map = builder.build();
        return new ServerLanguage() {
            @Override
            public Optional<String> get(@NotNull String key) {
                return Optional.ofNullable(map.get(key));
            }
        };
    }

    public static @Nonnull ServerLanguage getResourcePackDirLanguage(Path dir, String localeCode) {
        logger.info("getResourcePackDirLanguage: loading {}", dir);
        if (!Files.isDirectory(dir)) return ServerLanguage.empty();
        try (var paths = Files.list(dir)) {
            var validPaths = paths.filter(path -> !Files.isDirectory(path) && com.google.common.io.Files.getFileExtension(path.toString()).equals("zip"));
            return getResourcePackLanguage(validPaths::iterator, localeCode);
        } catch (IOException e) {
            logger.warn("Failed to list directory {}: ", dir, e);
            return ServerLanguage.empty();
        }
    }

    /**
     * Elements that appear earlier have higher priority and are applied first
     * */
    public static @Nonnull ServerLanguage compose(ServerLanguage... languages) {
        return new ServerLanguage() {
            @Override
            public Optional<String> get(@NotNull String key) {
                for (var language : languages) {
                    var ret = language.get(key);
                    if (ret.isPresent()) {
                        return ret;
                    }
                }
                return Optional.empty();
            }
        };
    }

    public static @Nonnull ServerLanguage empty() {
        return new ServerLanguage() {
            @Override
            public Optional<String> get(@NotNull String key) {
                return Optional.empty();
            }
        };
    }

    public static void loadFromResourcePack(InputStream stream, String localeCode, BiConsumer<String, String> output) throws IOException {
        var input = new ZipInputStream(stream);
        ZipEntry entry;
        while ((entry = input.getNextEntry()) != null) {
            var name = entry.getName();
            var regex = String.format("^assets/[^/]+/lang/%s.json$", localeCode); // assets/{namespace}/lang/{locale_code}.json
            var pattern = Pattern.compile(regex);
            var matcher = pattern.matcher(name);
            if (!matcher.find()) continue;
            logger.info("zip entry {}", name);
            try {
                Language.loadFromJson(input, output);
            } catch (Throwable e) {
                logger.warn("Failed to load {} in the resource pack: ", name, e);
            }
        }
    }
}
