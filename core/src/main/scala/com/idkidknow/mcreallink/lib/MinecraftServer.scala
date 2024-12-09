package com.idkidknow.mcreallink.lib

import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.mcreallink.utils.CallbackBundle
import fs2.Stream

object MinecraftServer {

  /** The Minecraft server may start and stop multiple times because the
   *  existance of single-player mode.
   *
   *  Await `MinecraftServer`([[MS]]) starting when performing the `Stream`'s `F`.
   *  When the server stops, all supervised fibers will be finalized.
   */
  def stream[F[_]: Concurrent, MS](
      serverStarting: CallbackBundle[F, MS, Unit],
      serverStopping: CallbackBundle[F, ?, Unit],
  ): Stream[F, (MS, Supervisor[F])] = {
    def makeSupervisor(ms: MS): F[(MS, Supervisor[F])] =
      Supervisor[F].allocated.map { case (supervisor, finalizer) =>
        serverStopping.registerRunOnce { _ =>
          finalizer
        }
        (ms, supervisor)
      }

    val s: Stream[F, MS] = serverStarting.registerAsStream(Queue.synchronous)
    s.evalMap(makeSupervisor)
  }
}
