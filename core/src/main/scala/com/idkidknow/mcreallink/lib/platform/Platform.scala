package com.idkidknow.mcreallink.lib.platform

import fs2.io.file.Path

/** DSL that encodes Minecraft & modloader specific types and functions
 *
 *  @tparam P
 *    Expression type of the platform's types, and `P[A]` will be interpreted as
 *    the actual type of the platform where `A` is a marker type.
 *  @tparam F
 *    The effect type
 */
trait Platform[P[_], F[_]] {
  val languageClass: LanguageClass[P, F]
  val componentClass: ComponentClass[P]
  val minecraftServerClass: MinecraftServerClass[P, F]
  val events: Events[P, F]

  def gameRootDirectory: Path
  def configDirectory: Path
}

object Platform {
  def apply[P[_], F[_]](using inst: Platform[P, F]) = inst
}
