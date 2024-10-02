package com.idkidknow.mcrealcomm.event

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [invoke] folds handlers' results by [defaultValue] and [foldFunction]
 *
 * the second argument of [foldFunction] is `() -> R` inside which calls a handler.
 * Short-circuiting by not invoking it.
 *  */
class SetEventManager<T, R>(val defaultValue: R, val foldFunction: (R, () -> R) -> R): EventManager<T, R> {
    private val handlers = mutableSetOf<EventHandler<T, R>>()
    private val lock = ReentrantLock()

    override fun addHandler(handler: EventHandler<T, R>) {
        lock.withLock { handlers.add(handler) }
    }

    override fun removeHandler(handler: EventHandler<T, R>) {
        lock.withLock { handlers.remove(handler) }
    }

    override fun invoke(event: T): R {
        var result = defaultValue
        lock.withLock {
            handlers.forEach { handler ->
                result = foldFunction(result) { handler.invoke(event) }
            }
        }
        return result
    }

    override fun clear() {
        lock.withLock { handlers.clear() }
    }
}

/**
 * Pass all events that [source] received to `this`
 *  */
class SetEventManagerProxy<T, R>(
    val source: EventManager<T, R>,
    val defaultValue: R,
    val foldFunction: (R, () -> R) -> R
): EventManager<T, R> by SetEventManager<T, R>(defaultValue, foldFunction) {
    init {
        source.addHandler(this::invoke)
    }

    fun removeProxy() {
        source.removeHandler(this::invoke)
    }
}

class SetUnitEventManager<T>: UnitEventManager<T> by SetEventManager<T, Unit>(Unit, { _, invokeNext -> invokeNext() })

/**
 * Pass all events that [source] received to `this`
 *  */
class SetUnitEventManagerProxy<T>(
    val source: UnitEventManager<T>,
): UnitEventManager<T> by SetUnitEventManager<T>() {
    init {
        source.addHandler(this::invoke)
    }

    fun removeProxy() {
        source.removeHandler(this::invoke)
    }
}

