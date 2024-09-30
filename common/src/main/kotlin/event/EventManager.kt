package com.idkidknow.mcrealcomm.event

typealias EventHandler<T> = (T) -> Unit

/**
 * Should be thread-safe.
 *
 * An EventManager itself is also an EventHandler, so
 * `manager1.addHandler(manager2)` will pass `manager1`'s received events
 * to `manager2`, which can be used to group handlers in `manager2`.
 *  */
interface EventManager<T>: (T) -> Unit {
    /** Should not block in handler. */
    fun addHandler(handler: EventHandler<T>)
    fun removeHandler(handler: EventHandler<T>)
    override fun invoke(event: T)
    fun clear()
}

class RegisteredEventHandler<T>(val handler: EventHandler<T>, val manager: EventManager<T>) {
    fun unregister() {
        manager.removeHandler(handler)
    }
}

fun <T> EventManager<T>.register(handler: EventHandler<T>): RegisteredEventHandler<T> {
    this.addHandler(handler)
    return RegisteredEventHandler(handler, this)
}

/**
 * Pass all events that [source] received to `this`
 *  */
class EventManagerProxy<T>(
    val source: EventManager<T>,
    val base: EventManager<T> = SetEventManager(),
): EventManager<T> by base {
    init {
        source.addHandler(this)
    }

    fun removeProxy() {
        source.removeHandler(this)
    }
}
