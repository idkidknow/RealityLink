package com.idkidknow.mcreallink.mixin.complement

import net.minecraft.network.chat.Component

/** @see com.idkidknow.mcreallink.mixin.mixin.PlayerListMixin */
object BroadcastingMessage {
    private val _ignoreMessage = ThreadLocal.withInitial { false }
    val ignoreMessage: Boolean get() = _ignoreMessage.get()
    var callback: (Component) -> Unit = {}

    fun ignoreTemporarily(action: () -> Unit) {
        _ignoreMessage.set(true)
        action()
        _ignoreMessage.set(false)
    }
}
