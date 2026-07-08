package com.idkidknow.realitylink.lib

import cats.effect.Concurrent
import cats.effect.std.Queue
import cats.effect.std.Supervisor
import cats.syntax.all.*
import com.idkidknow.realitylink.platform.MinecraftServer
import fs2.Stream

/** The Minecraft server may start and stop multiple times because the existence
 *  of single-player mode.
 *
 *  Await `MinecraftServer` starting when performing the `Stream`'s `F`. When
 *  the server stops, all supervised fibers will be finalized.
 */
def streamMinecraftServer[F[_]: Concurrent](
    serverStarting: CallbackBundle[F, MinecraftServer, Unit],
    serverStopping: CallbackBundle[F, ?, Unit],
): Stream[F, (MinecraftServer, Supervisor[F])] = {
  def makeSupervisor(ms: MinecraftServer): F[(MinecraftServer, Supervisor[F])] =
    Supervisor[F].allocated.flatMap { case (supervisor, finalizer) =>
      val registerFinalizer = serverStopping.registerRunOnce { _ =>
        finalizer
      }
      registerFinalizer *> (ms, supervisor).pure[F]
    }

  val s: Stream[F, MinecraftServer] =
    serverStarting.registerAsStream(Queue.synchronous)
  s.evalMap(makeSupervisor)
}
