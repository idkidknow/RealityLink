package com.idkidknow.realitylink.lib

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.effect.std.NonEmptyHotswap
import com.idkidknow.realitylink.platform.MinecraftServer

/** The Minecraft server may start and stop multiple times because the existence
 *  of single-player mode.
 *
 *  Acquire `main` when `MinecraftServer` starts. When the server stops, the
 *  acquired resource will be finalized.
 */
def manageMinecraftServer[F[_]: Concurrent](
    serverStarting: CallbackBundle[F, MinecraftServer, Unit],
    serverStopping: CallbackBundle[F, Unit, Unit],
)(main: MinecraftServer => Resource[F, Unit]): Resource[F, Unit] =
  for {
    current <- NonEmptyHotswap[F, Unit](Resource.unit[F])
    _ <- serverStopping.registerAsResource { _ =>
      current.swap(Resource.unit[F])
    }
    _ <- serverStarting.registerAsResource { server =>
      current.swap(main(server))
    }
  } yield ()
