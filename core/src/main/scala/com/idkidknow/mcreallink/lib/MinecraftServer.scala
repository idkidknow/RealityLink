package com.idkidknow.mcreallink.lib

import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.effect.std.Supervisor
import cats.syntax.all.*
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
      Supervisor[F].allocated.flatMap { case (supervisor, finalizer) =>
        val registerFinalizer = serverStopping.registerRunOnce { _ =>
          finalizer
        }
        registerFinalizer *> (ms, supervisor).pure[F]
      }

    val s: Stream[F, MS] = serverStarting.registerAsStream(Queue.synchronous)
    s.evalMap(makeSupervisor)
  }
}
