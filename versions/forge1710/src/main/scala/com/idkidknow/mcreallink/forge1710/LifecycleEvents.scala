package com.idkidknow.mcreallink.forge1710

import net.minecraft.server.MinecraftServer

@SuppressWarnings(Array("org.wartremover.warts.Var"))
object LifecycleEvents {
  var serverStartingCallback: MinecraftServer => Unit = { _ => () }
  var serverStoppingCallback: () => Unit = { () => () }
}
