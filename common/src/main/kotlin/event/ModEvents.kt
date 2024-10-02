package com.idkidknow.mcrealcomm.event

import net.minecraft.network.chat.Component
import net.minecraft.server.players.PlayerList

data class BroadcastingMessageEvent(val message: Component)

object BroadCastingMessageEventManager:
    UnitEventManager<BroadcastingMessageEvent> by SetUnitEventManager<BroadcastingMessageEvent>() {

    val ignored: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
}

inline fun PlayerList.noBroadCastingMessageEventCurrentThread(action: PlayerList.() -> Unit) {
    BroadCastingMessageEventManager.ignored.set(true)
    this.action()
    BroadCastingMessageEventManager.ignored.set(false)
}

object CallingStartCommandEventManager:
    EventManager<Unit, Exception?> by SetEventManager(null, { result, invokeNext ->
        result ?: invokeNext()
    })

object CallingStopCommandEventManager:
    UnitEventManager<Unit> by SetUnitEventManager()

data class ModEvents(
    val broadcastingMessage: UnitEventManager<BroadcastingMessageEvent> = BroadCastingMessageEventManager,
    val callingStartCommand: EventManager<Unit, Exception?> = CallingStartCommandEventManager,
    val callingStopCommand: UnitEventManager<Unit> = CallingStopCommandEventManager,
)
