package com.idkidknow.mcrealcomm

import com.idkidknow.mcreallink.lib.platform.Component
import com.idkidknow.mcreallink.utils.CallbackBundle

class ModEvents[P[_], F[_]](
    val callingStartCommand: CallbackBundle[F, Unit, Either[Throwable, Unit]],
    val callingStopCommand: CallbackBundle[F, Unit, Unit],
    val broadcastingMessage: CallbackBundle[F, P[Component], Unit],
)
