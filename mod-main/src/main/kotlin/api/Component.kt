package com.idkidknow.mcreallink.api

interface ComponentClass<T, TLanguage, TMinecraftServer> {
    fun T.translateWith(language: TLanguage): String
    fun T.serialize(server: TMinecraftServer): String
    fun deserialize(json: String, server: TMinecraftServer): T
    fun literal(text: String): T
}
