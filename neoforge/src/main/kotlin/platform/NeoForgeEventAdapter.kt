package com.idkidknow.mcrealcomm.neoforge.platform

import com.idkidknow.mcrealcomm.event.SetUnitEventManager
import com.idkidknow.mcrealcomm.event.UnitEventHandler
import com.idkidknow.mcrealcomm.event.UnitEventManager
import net.neoforged.bus.api.Event
import net.neoforged.neoforge.common.NeoForge

class NeoForgeEventAdapter<T, E: Event>(
    base: UnitEventManager<T>,
    clazz: Class<E>,
    adapter: (UnitEventHandler<T>) -> (E) -> Unit,
): UnitEventManager<T> by base {
    init {
        NeoForge.EVENT_BUS.addListener<E>(clazz, adapter { this.invoke(it) })
    }
}

fun <T, E: Event>eventManagerFromNeoForge(clazz: Class<E>, adapter: (UnitEventHandler<T>) -> (E) -> Unit): UnitEventManager<T> {
    return NeoForgeEventAdapter(SetUnitEventManager<T>(), clazz, adapter)
}

inline fun <T, reified E: Event>eventManagerFromNeoForge(noinline adapter: (UnitEventHandler<T>) -> (E) -> Unit): UnitEventManager<T> {
    return eventManagerFromNeoForge(E::class.java, adapter)
}
