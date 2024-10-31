package com.idkidknow.mcreallink.platform.neoforge121.impl

import com.idkidknow.mcreallink.api.BroadcastingMessageEvent
import com.idkidknow.mcreallink.api.CallbackRegistry
import com.idkidknow.mcreallink.api.CallbackSet
import com.idkidknow.mcreallink.api.CallingStartCommandEvent
import com.idkidknow.mcreallink.api.CallingStopCommandEvent
import com.idkidknow.mcreallink.api.Events
import com.idkidknow.mcreallink.api.ServerStartingEvent
import com.idkidknow.mcreallink.api.ServerStoppingEvent
import com.idkidknow.mcreallink.api.UnitCallbackRegistry
import com.idkidknow.mcreallink.api.UnitCallbackSet
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class EventsImpl(invoker: EventsInvoker) : Events<Component, MinecraftServer> {
    override val serverStarting: UnitCallbackRegistry<ServerStartingEvent<MinecraftServer>> = invoker.serverStarting
    override val serverStopping: UnitCallbackRegistry<ServerStoppingEvent<MinecraftServer>> = invoker.serverStopping
    override val callingStartCommand: CallbackRegistry<CallingStartCommandEvent, Result<Unit>> = invoker.callingStartCommand
    override val callingStopCommand: UnitCallbackRegistry<CallingStopCommandEvent> = invoker.callingStopCommand
    override val broadcastingMessage: UnitCallbackRegistry<BroadcastingMessageEvent<Component>> = invoker.broadcastingMessage
}

class EventsInvoker {
    val serverStarting: UnitCallbackSet<ServerStartingEvent<MinecraftServer>> = UnitCallbackSet()
    val serverStopping: UnitCallbackSet<ServerStoppingEvent<MinecraftServer>> = UnitCallbackSet()
    val callingStartCommand: CallbackSet<CallingStartCommandEvent, Result<Unit>> = CallbackSet()
    val callingStopCommand: UnitCallbackSet<CallingStopCommandEvent> = UnitCallbackSet()
    val broadcastingMessage: UnitCallbackSet<BroadcastingMessageEvent<Component>> = UnitCallbackSet()
}
