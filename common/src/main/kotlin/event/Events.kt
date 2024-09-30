package com.idkidknow.mcrealcomm.event

import net.minecraft.network.chat.Component

data class BroadcastingMessageEvent(val message: Component)

val broadcastingMessageEventManager = SetEventManager<BroadcastingMessageEvent>()
