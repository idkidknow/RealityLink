package com.idkidknow.mcrealcomm.event

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SetEventManager<T>: EventManager<T> {
    private val handlers = mutableSetOf<EventHandler<T>>()
    private val lock = ReentrantLock()

    override fun addHandler(handler: EventHandler<T>) {
        lock.withLock { handlers.add(handler) }
    }

    override fun removeHandler(handler: EventHandler<T>) {
        lock.withLock { handlers.remove(handler) }
    }

    override fun invoke(event: T) {
        lock.withLock { handlers.forEach { it.invoke(event) } }
    }

    override fun clear() {
        lock.withLock { handlers.clear() }
    }
}
