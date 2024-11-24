package com.idkidknow.mcreallink.forge118.platform

import cats.effect.IO
import cats.effect.std.Dispatcher
import com.idkidknow.mcreallink.forge118.mixin.complement.{BroadcastingMessage, ServerTranslate}
import com.idkidknow.mcreallink.lib.platform.{ComponentClass, Events, LanguageClass, MinecraftServerClass, Platform as PlatformTrait}
import fs2.io.file.Path
import fs2.Stream
import net.minecraft.locale.Language
import net.minecraft.network.chat.{Component, FormattedText, TextComponent}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.FormattedCharSequence
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.server.{ServerStartingEvent, ServerStoppingEvent}
import net.minecraftforge.fml.loading.FMLPaths

type Platform[A] = A match {
  case com.idkidknow.mcreallink.lib.platform.Language => Language
  case com.idkidknow.mcreallink.lib.platform.Component => Component
  case com.idkidknow.mcreallink.lib.platform.MinecraftServer => MinecraftServer
}

object Platform {
  given instPlatform: PlatformTrait[Platform, IO] = new PlatformTrait[Platform, IO] {

    override val languageClass: LanguageClass[Platform, IO] = new LanguageClass[Platform, IO] {
      override def get(language: Language, key: String): Option[String] = if (language.has(key)) {
        Some(language.getOrDefault(key))
      } else {
        None
      }

      override def create(map: String => Option[String]): Language = new Language {
        override def getOrDefault(id: String): String = map(id).getOrElse(id)
        override def has(id: String): Boolean = map(id).isDefined
        // only used in GUI and has no effects on server
        override def isDefaultRightToLeft: Boolean = false
        // only used in GUI
        override def getVisualOrder(text: FormattedText): FormattedCharSequence = FormattedCharSequence.EMPTY
      }

      override def classLoaderResourceStream(path: String): Stream[IO, Byte] =
        fs2.io.readClassLoaderResource(path, classLoader = classOf[Language].getClassLoader)
      override def parseLanguageFile(stream: Stream[IO, Byte]): IO[Option[Map[String, String]]] =
        fs2.io.toInputStreamResource(stream).use { stream =>
          IO {
            val map = scala.collection.mutable.HashMap[String, String]()
            try {
              Language.loadFromJson(stream, map.put)
              Some(map.toMap)
            } catch {
              case _: Exception => None
            }
          }
        }
    }

    override val componentClass: ComponentClass[Platform] = new ComponentClass[Platform] {
      override def translateWith(component: Component, language: Language): String =
        ServerTranslate.translate(component, language)

      override def literal(text: String): Component = TextComponent(text)
    }

    override val minecraftServerClass: MinecraftServerClass[Platform, IO] = new MinecraftServerClass[Platform, IO] {
      override def resourceNamespaces(server: MinecraftServer): IO[Iterable[String]] = IO {
        import scala.jdk.CollectionConverters.*
        server.getResourceManager.getNamespaces.asScala
      }
    }

    override val events: Events[Platform, IO] = new Events[Platform, IO] {
      override def onServerStarting(dispatcher: Dispatcher[IO])(action: MinecraftServer => IO[Unit]): IO[Unit] = IO {
        MinecraftForge.EVENT_BUS.addListener[ServerStartingEvent] { event =>
          dispatcher.unsafeRunSync(action(event.getServer))
        }
      }

      override def onServerStopping(dispatcher: Dispatcher[IO])(action: MinecraftServer => IO[Unit]): IO[Unit] = IO {
        MinecraftForge.EVENT_BUS.addListener[ServerStoppingEvent] { event =>
          dispatcher.unsafeRunSync(action(event.getServer))
        }
      }
      
      override def onCallingStartCommand(dispatcher: Dispatcher[IO])(action: () => IO[Either[Throwable, Unit]]): IO[Unit] = IO {
        ???
      }

      override def onCallingStopCommand(dispatcher: Dispatcher[IO])(action: () => IO[Unit]): IO[Unit] = ???

      override def onBroadcastingMessage(dispatcher: Dispatcher[IO])(action: Component => IO[Unit]): IO[Unit] = IO {
        BroadcastingMessage.callback = { component =>
          dispatcher.unsafeRunAndForget(action(component))
        }
      }
    }

    override def gameRootDirectory: Path = Path.fromNioPath(FMLPaths.GAMEDIR.get())

    override def configDirectory: Path = Path.fromNioPath(FMLPaths.CONFIGDIR.get())
  }
}
