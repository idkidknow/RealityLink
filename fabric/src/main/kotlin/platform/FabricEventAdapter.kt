package com.idkidknow.mcrealcomm.fabric.platform

import com.idkidknow.mcrealcomm.event.SetUnitEventManager
import com.idkidknow.mcrealcomm.event.UnitEventHandler
import com.idkidknow.mcrealcomm.event.UnitEventManager
import net.fabricmc.fabric.api.event.Event

class FabricEventAdapter<T, E>(
    base: UnitEventManager<T>,
    fabricEvent: Event<E>,
    adapter: (UnitEventHandler<T>) -> E,
): UnitEventManager<T> by base {
    init {
        fabricEvent.register(adapter { this.invoke(it) })
    }
}

fun <T, E>eventManagerFromFabric(fabricEvent: Event<E>, adapter: (UnitEventHandler<T>) -> E): UnitEventManager<T> {
    return FabricEventAdapter(SetUnitEventManager<T>(), fabricEvent, adapter)
}
