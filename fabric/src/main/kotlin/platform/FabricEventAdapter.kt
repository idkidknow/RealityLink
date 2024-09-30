package com.idkidknow.mcrealcomm.fabric.platform

import com.idkidknow.mcrealcomm.event.EventHandler
import com.idkidknow.mcrealcomm.event.EventManager
import com.idkidknow.mcrealcomm.event.SetEventManager
import net.fabricmc.fabric.api.event.Event

class FabricEventAdapter<T, E>(
    base: EventManager<T>,
    fabricEvent: Event<E>,
    adapter: (EventHandler<T>) -> E,
): EventManager<T> by base {
    init {
        fabricEvent.register(adapter { this.invoke(it) })
    }
}

fun <T, E>eventManagerFromFabric(fabricEvent: Event<E>, adapter: (EventHandler<T>) -> E): EventManager<T> {
    return FabricEventAdapter(SetEventManager<T>(), fabricEvent, adapter)
}
