package com.idkidknow.mcreallink

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import com.idkidknow.mcreallink.lib.platform.Component
import com.idkidknow.mcreallink.utils.CallbackBundle
import fs2.Stream

class ModEvents[P[_], F[_]](
    val callingStartCommand: CallbackBundle[F, Unit, Either[Throwable, Unit]],
    val callingStopCommand: CallbackBundle[F, Unit, Unit],
    val broadcastingMessage: CallbackBundle[F, P[Component], Unit],
)

object ModEvents {
  def apply[P[_], F[_]](using inst: ModEvents[P, F]): ModEvents[P, F] = inst

  def chatStream[P[_], F[_]: Concurrent](using
      ModEvents[P, F]
  ): Resource[F, Stream[F, P[Component]]] =
    ModEvents[P, F].broadcastingMessage.asStream
}
