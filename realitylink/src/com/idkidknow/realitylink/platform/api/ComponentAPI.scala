package com.idkidknow.realitylink.platform.api

trait ComponentAPI extends Types {
  trait ComponentOps {
    def literal(text: String): Component
    def deserialize(json: String, server: MinecraftServer): Option[Component]

    extension (c: Component) {
      def translateWith(language: Language): String
      def serialize(server: MinecraftServer): String
    }
  }

  val Component: ComponentOps
}
