package com.idkidknow.mcrealcomm.context

import com.idkidknow.mcrealcomm.api.CallbackRegistry
import com.idkidknow.mcrealcomm.api.CallbackSet
import com.idkidknow.mcrealcomm.api.UnitCallbackRegistry
import com.idkidknow.mcrealcomm.api.UnitCallbackSet
import com.idkidknow.mcrealcomm.platform.BroadcastingMessageEvent

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
