package com.idkidknow.mcreallink.api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * callbacks of `(T) -> R`
 *
 * thread-safe
 * */
class CallbackSet<T, R> : CallbackRegistry<T, R> {
    private val callbacks = mutableSetOf<(T) -> R>()
    private val lock = Mutex()

    val registry: CallbackRegistry<T, R> get() = this

    override fun addCallback(callback: (T) -> R) {
        runBlocking { lock.withLock {
            callbacks.add(callback)
        }}
    }

    override fun removeCallback(callback: (T) -> R) {
        runBlocking { lock.withLock {
            callbacks.remove(callback)
        }}
    }

    fun clear() {
        runBlocking { lock.withLock {
            callbacks.clear()
        }}
    }

    /**
     * fold callbacks' results by [initState] and [foldFunction]
     *
     * The second argument of [foldFunction] is `() -> R` inside which calls next callback.
     * This lazy evaluation allows something like short-circuiting.
     * */
    fun <S> invokeFold(arg: T, initState: S, foldFunction: (S, () -> R) -> S): S {
        var ret: S = initState
        runBlocking { lock.withLock {
            for (callback in callbacks) {
                ret = foldFunction(ret) { callback.invoke(arg) }
            }
        }}
        return ret
    }

    /** Invokes all callbacks and returns the last one's result */
    fun invokeAll(arg: T, defaultValue: R): R = invokeFold(arg, defaultValue) { _, invokeNext ->
        invokeNext()
    }
}

interface CallbackRegistry<T, R> {
    fun addCallback(callback: (T) -> R)
    fun removeCallback(callback: (T) -> R)
}

fun <T, R> CallbackRegistry<T, R>.register(callback: (T) -> R): RegisteredCallback<T, R> {
    addCallback(callback)
    return RegisteredCallback(this, callback)
}

/** Bundled callback which is easy to be removed from the registry */
class RegisteredCallback<T, R>(val registry: CallbackRegistry<T, R>, val callback: (T) -> R) {
    fun unregister() {
        registry.removeCallback(callback)
    }
}

typealias UnitCallbackSet<T> = CallbackSet<T, Unit>
typealias UnitCallbackRegistry<T> = CallbackRegistry<T, Unit>

fun <T> UnitCallbackSet<T>.invoke(arg: T) {
    invokeAll(arg, Unit)
}
