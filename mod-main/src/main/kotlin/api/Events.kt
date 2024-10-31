package com.idkidknow.mcreallink.api


data class ServerStartingEvent<TMinecraftServer>(val server: TMinecraftServer)
data class ServerStoppingEvent<TMinecraftServer>(val server: TMinecraftServer)
object CallingStartCommandEvent
object CallingStopCommandEvent
data class BroadcastingMessageEvent<TComponent>(val message: TComponent)

interface Events<TComponent, TMinecraftServer> {
    val serverStarting: UnitCallbackRegistry<ServerStartingEvent<TMinecraftServer>>
    val serverStopping: UnitCallbackRegistry<ServerStoppingEvent<TMinecraftServer>>
    val callingStartCommand: CallbackRegistry<CallingStartCommandEvent, Result<Unit>>
    val callingStopCommand: UnitCallbackRegistry<CallingStopCommandEvent>
    val broadcastingMessage: UnitCallbackRegistry<BroadcastingMessageEvent<TComponent>>
}
