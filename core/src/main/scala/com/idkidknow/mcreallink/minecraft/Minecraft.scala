package com.idkidknow.mcreallink.minecraft

import java.nio.file.Path
import java.io.InputStream

/** Interact with Minecraft and modloader. Resembles the Minecraft API (mojmap)
 *  and easy to implement with zero dependencies.
 */
trait Minecraft {
  /** [[net.minecraft.network.chat.Component]] */
  type Component
  /** [[net.minecraft.locale.Language]] */
  type Language
  /** [[net.minecraft.server.MinecraftServer]] */
  type MinecraftServer

  trait ComponentClass {
    def translateWith(component: Component, language: Language): String
    def literal(text: String): Component
    def serialize(component: Component, server: MinecraftServer): String
    def deserialize(json: String, server: MinecraftServer): Option[Component]
  }

  trait LanguageClass {
    def get(language: Language, key: String): Option[String]
    def make(map: String => Option[String]): Language
    /** `Language.getClassLoader().getResourceAsStream(path)` */
    def classLoaderResourceStream(path: String): InputStream
    /** `.lang` before 1.13 and `.json` after 1.13 */
    def parseLanguageFile(stream: InputStream): Option[Map[String, String]]
  }

  trait MinecraftServerClass {
    /** `.getResourceManager().getNamespaces()` */
    def resourceNamespaces(server: MinecraftServer): Iterable[String]
    def broadcastMessage(server: MinecraftServer, message: Component): Unit
  }

  // Immitate Minecraft classes
  val Component: ComponentClass
  val Language: LanguageClass
  val MinecraftServer: MinecraftServerClass

  trait Events {
    /** `ServerstartingEvent`, `ServerLifecycleEvents.SERVER_STARTING` */
    def onServerStarting(action: MinecraftServer => Unit): Unit
    /** `ServerstoppingEvent`, `ServerLifecycleEvents.SERVER_STOPPING` */
    def onServerStopping(action: MinecraftServer => Unit): Unit

    def setOnCallingStartCommand(action: () => Either[Throwable, Unit]): Unit
    def setOnCallingStopCommand(action: () => Unit): Unit
    def setOnBroadcastingMessage(action: Component => Unit): Unit
  }

  // Modifications
  val events: Events

  def gameRootDirectory: Path
  def configDirectory: Path
}