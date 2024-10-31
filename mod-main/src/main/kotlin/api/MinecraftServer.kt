package com.idkidknow.mcreallink.api

interface MinecraftServerClass<T, TComponent> {
    val T.namespaces: Iterable<String>
    fun T.broadcastMessageWithoutCallback(message: TComponent)
}
