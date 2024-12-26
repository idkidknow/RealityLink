package com.idkidknow.mcreallink.forge1182.mixin.complement

import net.minecraft.network.chat.Component

object BroadcastingMessage {
  private val _ignoreMessage = ThreadLocal.withInitial(() => false)
  def ignoreMessage: Boolean = _ignoreMessage.get()
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  var callback: Component => Unit = { _ => () }

  def ignoreTemporarily(action: () => Unit): Unit = {
    _ignoreMessage.set(true)
    action()
    _ignoreMessage.set(false)
  }
}
