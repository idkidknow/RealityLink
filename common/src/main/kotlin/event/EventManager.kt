package com.idkidknow.mcrealcomm.event

typealias EventHandler<T, R> = (T) -> R
typealias UnitEventHandler<T> = (T) -> Unit
typealias UnitEventManager<T> = EventManager<T, Unit>

/**
 * Manage a bunch of callbacks and be able to invoke all of them somewhere an event happens.
 * (observer pattern)
 *
 * Should be thread-safe.
 *
 * [T]: event type, [R]: event result type
 *
 * An EventManager itself is also a handler (`(T) -> R`), so
 * `manager1.addHandler(manager2)` will pass `manager1`'s received events
 * to `manager2`, which can be used to group handlers in `manager2`.
 * See [EventManagerProxy]
 *  */
interface EventManager<T, R>: (T) -> R {
    /** Should not block in handler. */
    fun addHandler(handler: (T) -> R)
    fun removeHandler(handler: (T) -> R)
    override fun invoke(event: T): R
    fun clear()
}

/** A wrapper class to remove a handler from the manager easily */
class RegisteredEventHandler<T, R>(val handler: EventHandler<T, R>, val manager: EventManager<T, R>) {
    fun unregister() {
        manager.removeHandler(handler)
    }
}

fun <T, R> EventManager<T, R>.register(handler: EventHandler<T, R>): RegisteredEventHandler<T, R> {
    this.addHandler(handler)
    return RegisteredEventHandler(handler, this)
}

/**
 * Pass all events that [source] received to `this`
 *  */
class EventManagerProxy<T, R>(
    val source: EventManager<T, R>,
    val base: EventManager<T, R>,
): EventManager<T, R> by base {
    init {
        source.addHandler(this)
    }

    fun removeProxy() {
        source.removeHandler(this)
    }
}
