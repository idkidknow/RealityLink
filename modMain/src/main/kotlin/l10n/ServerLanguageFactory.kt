package com.idkidknow.mcreallink.l10n

import com.google.gson.JsonParseException
import com.idkidknow.mcreallink.l10n.ServerLanguage
import io.github.oshai.kotlinlogging.KotlinLogging
import net.minecraft.locale.Language
import net.minecraft.server.MinecraftServer
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.zip.ZipInputStream
import kotlin.io.path.isDirectory

val logger = KotlinLogging.logger {}

sealed class LanguageLoadingException(message: String, cause: Throwable): Exception(message, cause) {
    data class IOException(val msg: String, val e: java.io.IOException) : LanguageLoadingException(msg, e)
    data class ParseException(val msg: String, val e: JsonParseException) : LanguageLoadingException(msg, e)
}

object ServerLanguageFactory {
    /**
     * Read language files in Java resources
     *
     * Under normal circumstances, there are Minecraft's en_us.json, and language jsons in every mods' jar file
     * */
    fun fromJavaResource(namespaces: Iterable<String>, localeCode: String): ServerLanguage {
        var map: MutableMap<String, String> = mutableMapOf()
        for (namespace in namespaces) {
            val path = "/assets/$namespace/lang/$localeCode.json"
            logger.debug { "Trying to load Java resources $path" }
            Language::class.java.getResourceAsStream(path).use { stream ->
                // ignore it if not found
                if (stream == null) return@use
                Language.loadFromJson(stream, map::put) // Just throw if there's problem in resources. That's not my problem.
                logger.info { "Loaded Java resources $path" }
            }
        }
        return object: ServerLanguage() {
            override fun get(key: String): Optional<String> = Optional.ofNullable(map[key])
        }
    }

    fun fromJavaResource(server: MinecraftServer, localeCode: String): ServerLanguage =
        fromJavaResource(server.resourceManager.namespaces, localeCode)

    private fun loadFromResourcePack(
        input: InputStream,
        localeCode: String,
        output: (String, String) -> Unit,
    ): Result<Unit> {
        try {
            val input = ZipInputStream(input)
            generateSequence { input.nextEntry }.forEach { entry ->
                val name = entry.name
                val regex = "^assets/[^/]+/lang/$localeCode.json$".toRegex()
                if (!regex.matches(name)) return@forEach
                logger.debug { "Trying to load $name in the zip" }
                try {
                    Language.loadFromJson(input, output)
                } catch (e: JsonParseException) {
                    return Result.failure(LanguageLoadingException.ParseException("Failed to load entry $name", e))
                }
            }
        } catch (e: IOException) {
            return Result.failure(LanguageLoadingException.IOException("IO error", e))
        }
        return Result.success(Unit)
    }

    fun fromResourcePack(paths: Iterable<Path>, localeCode: String): Result<ServerLanguage> {
        try {
            var map: MutableMap<String, String> = mutableMapOf()
            for (path in paths) {
                logger.debug { "Trying to load resource pack $path" }
                try {
                    Files.newInputStream(path).buffered().use { stream ->
                        loadFromResourcePack(stream, localeCode, map::put).getOrThrow()
                    }
                } catch (e: IOException) {
                    return Result.failure(LanguageLoadingException.IOException("Failed to read $path", e))
                }
                logger.info { "Loaded resource pack $path" }
            }
            return Result.success(object: ServerLanguage() {
                override fun get(key: String): Optional<String> = Optional.ofNullable(map[key])
            })
        } catch (e: LanguageLoadingException) {
            return Result.failure(e)
        }
    }

    fun fromResourcePackDir(path: Path, localeCode: String): Result<ServerLanguage> {
        if (!Files.isDirectory(path)) {
            return Result.failure(LanguageLoadingException.IOException("$path is not a directory", IOException()))
        }
        try {
            val paths = Files.list(path).use {
                it.filter { filepath ->
                    !filepath.isDirectory() && filepath.toFile().extension == "zip"
                }.toList()
            }
            return fromResourcePack(paths, localeCode)
        } catch (e: IOException) {
            return Result.failure(LanguageLoadingException.IOException("Failed to list $path", e))
        }
    }

    /** That appear earlier have higher priority and are applied first */
    fun compose(vararg languages: ServerLanguage): ServerLanguage = object: ServerLanguage() {
        override fun get(key: String): Optional<String> {
            for (language in languages) {
                var ret = language.get(key)
                if (ret.isPresent) return ret
            }
            return Optional.empty()
        }
    }
}
