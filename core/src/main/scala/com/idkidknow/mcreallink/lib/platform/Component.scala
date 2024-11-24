package com.idkidknow.mcreallink.lib.platform

/** [[net.minecraft.network.chat.Component]] */
type Component = Component.type

trait ComponentClass[P[_]] {
  def translateWith(component: P[Component], language: P[Language]): String
  def literal(text: String): P[Component]
}

object Component {
  def apply[P[_]](using inst: ComponentClass[P]): ComponentClass[P] = inst
}

object ComponentClass {
  given platformComponentClass[P[_], F[_]](using
      p: Platform[P, F],
  ): ComponentClass[P] = p.componentClass
}
