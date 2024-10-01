package com.idkidknow.mcrealcomm.neoforge.platform

import com.idkidknow.mcrealcomm.event.EventHandler
import com.idkidknow.mcrealcomm.event.EventManager
import com.idkidknow.mcrealcomm.event.SetEventManager
import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge

class NeoForgeEventAdapter<T, E: Event>(
    base: EventManager<T>,
    clazz: Class<E>,
    adapter: (EventHandler<T>) -> (E) -> Unit,
): EventManager<T> by base {
    init {
        NeoForge.EVENT_BUS.addListener<E>(clazz, adapter { this.invoke(it) })
    }
}

fun <T, E: Event>eventManagerFromNeoForge(clazz: Class<E>, adapter: (EventHandler<T>) -> (E) -> Unit): EventManager<T> {
    return NeoForgeEventAdapter(SetEventManager<T>(), clazz, adapter)
}

inline fun <T, reified E: Event>eventManagerFromNeoForge(noinline adapter: (EventHandler<T>) -> (E) -> Unit): EventManager<T> {
    return eventManagerFromNeoForge(E::class.java, adapter)
}
