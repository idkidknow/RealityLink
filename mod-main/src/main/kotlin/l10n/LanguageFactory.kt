package com.idkidknow.mcreallink.l10n

import com.idkidknow.mcreallink.platform.Platform
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import kotlin.io.path.isDirectory

val logger = KotlinLogging.logger {}

sealed class LanguageLoadingException(message: String, cause: Throwable): Exception(message, cause) {
    data class IOException(val msg: String, val e: java.io.IOException) : LanguageLoadingException(msg, e)
    data class ParseException(val msg: String, val e: Exception) : LanguageLoadingException(msg, e)
}

class LanguageFactory<TComponent, TLanguage, TMinecraftServer>(val platform: Platform<TComponent, TLanguage, TMinecraftServer>) {
    /**
     * Read language files in Java resources
     *
     * Under normal circumstances, there are Minecraft's en_us.json, and language jsons in every mods' jar file
     * */
    fun fromJavaResource(namespaces: Iterable<String>, localeCode: String): TLanguage {
        var map: MutableMap<String, String> = mutableMapOf()
        for (namespace in namespaces) {
            val path = "/assets/$namespace/lang/$localeCode.json"
            logger.debug { "Trying to load Java resources $path" }
            platform.minecraftClassLoader.getResourceAsStream(path).use { stream ->
                // ignore it if not found
                if (stream == null) return@use
                platform.instLanguage.parseLanguageFile(stream, map::put) // Just throw if there's problem in resources. That's not my problem.
                logger.info { "Loaded Java resources $path" }
            }
        }
        return platform.instLanguage.create { map[it] }
    }

    fun fromJavaResource(server: TMinecraftServer, localeCode: String): TLanguage =
        fromJavaResource(with(platform.instMinecraftServer) { server.namespaces }, localeCode)

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
                    platform.instLanguage.parseLanguageFile(input, output)
                } catch (e: Exception) {
                    return Result.failure(LanguageLoadingException.ParseException("Failed to load entry $name", e))
                }
            }
        } catch (e: IOException) {
            return Result.failure(LanguageLoadingException.IOException("IO error", e))
        }
        return Result.success(Unit)
    }

    fun fromResourcePack(paths: Iterable<Path>, localeCode: String): Result<TLanguage> {
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
            return Result.success(platform.instLanguage.create { map[it] })
        } catch (e: LanguageLoadingException) {
            return Result.failure(e)
        }
    }

    fun fromResourcePackDir(path: Path, localeCode: String): Result<TLanguage> {
        if (!Files.isDirectory(path)) {
            return Result.failure(LanguageLoadingException.IOException("$path is not a directory", IOException()))
        }
        try {
            val paths: List<Path> = Files.list(path).use {
                it.filter { filepath ->
                    !filepath.isDirectory() && filepath.toFile().extension == "zip"
                }.collect(Collectors.toList())
            }
            return fromResourcePack(paths, localeCode)
        } catch (e: IOException) {
            return Result.failure(LanguageLoadingException.IOException("Failed to list $path", e))
        }
    }

    /** That appear earlier have higher priority and are applied first */
    fun compose(vararg languages: TLanguage): TLanguage = with(platform.instLanguage) { create { key ->
        for (language in languages) {
            val ret = language.get(key)
            if (ret != null) return@create ret
        }
        return@create null
    }}
}
