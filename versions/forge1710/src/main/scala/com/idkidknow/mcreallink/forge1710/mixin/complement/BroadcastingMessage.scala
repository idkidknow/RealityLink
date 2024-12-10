package com.idkidknow.mcreallink.forge1710.mixin.complement

import net.minecraft.util.IChatComponent

object BroadcastingMessage {
  private val _ignoreMessage = ThreadLocal.withInitial(() => false)
  def ignoreMessage: Boolean = _ignoreMessage.get()
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  var callback: IChatComponent => Unit = { _ => () }
  def getCallback: java.util.function.Consumer[IChatComponent] = { c => callback(c) }

  def ignoreTemporarily(action: () => Unit): Unit = {
    _ignoreMessage.set(true)
    action()
    _ignoreMessage.set(false)
  }
}
