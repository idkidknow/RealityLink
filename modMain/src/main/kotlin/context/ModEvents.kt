package com.idkidknow.mcreallink.context

import com.idkidknow.mcreallink.api.CallbackRegistry
import com.idkidknow.mcreallink.api.CallbackSet
import com.idkidknow.mcreallink.api.UnitCallbackRegistry
import com.idkidknow.mcreallink.api.UnitCallbackSet
import com.idkidknow.mcreallink.platform.BroadcastingMessageEvent

object CallingStartCommandEvent
object CallingStopCommandEvent

interface ModEvents {
    val broadcastingMessage: UnitCallbackRegistry<BroadcastingMessageEvent>
    val callingStartCommand: CallbackRegistry<CallingStartCommandEvent, Exception?>
    val callingStopCommand: UnitCallbackRegistry<CallingStopCommandEvent>
}

class ModEventsInvoker : ModEvents {
    override val broadcastingMessage: UnitCallbackSet<BroadcastingMessageEvent> = UnitCallbackSet()
    override val callingStartCommand: CallbackSet<CallingStartCommandEvent, Exception?> = CallbackSet()
    override val callingStopCommand: UnitCallbackSet<CallingStopCommandEvent> = CallbackSet()
}
