package com.idkidknow.mcrealcomm.event

import net.minecraft.network.chat.Component
import net.minecraft.server.players.PlayerList

data class BroadcastingMessageEvent(val message: Component)

object BroadCastingMessageEventManager: EventManager<BroadcastingMessageEvent> by SetEventManager<BroadcastingMessageEvent>() {
    val ignored: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
}

inline fun PlayerList.noBroadCastingMessageEventCurrentThread(action: PlayerList.() -> Unit) {
    BroadCastingMessageEventManager.ignored.set(true)
    this.action()
    BroadCastingMessageEventManager.ignored.set(false)
}
