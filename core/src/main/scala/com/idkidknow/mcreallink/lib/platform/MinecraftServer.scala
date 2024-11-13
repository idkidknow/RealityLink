package com.idkidknow.mcreallink.lib.platform

/** [[net.minecraft.server.MinecraftServer]] */
type MinecraftServer = MinecraftServer.type

trait MinecraftServerClass[P[_], F[_]] {
  def resourceNamespaces: F[Iterable[String]]
}

object MinecraftServer {
  def apply[P[_], F[_]](using inst: MinecraftServerClass[P, F]) = inst
}

object MinecraftServerClass {
  given platformMinecraftServerClass[P[_], F[_]](using
      p: Platform[P, F],
  ): MinecraftServerClass[P, F] = p.minecraftServerClass
}
