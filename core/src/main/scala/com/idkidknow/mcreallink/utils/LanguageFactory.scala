package com.idkidknow.mcrealcomm.utils

import cats.Monad
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.platform.Language
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import com.idkidknow.mcreallink.lib.platform.Platform
import org.typelevel.log4cats.Logger

object LanguageFactory {
  def fromJavaResource[P[_], F[_]: Monad: Logger](
      namespaces: Iterable[String],
      localeCode: String,
  )(using Platform[P, F]): F[P[Language]] = {
    def load(namespace: String, localeCode: String): F[Map[String, String]] = {
      val path = s"/assets/$namespace/lang/$localeCode.json"
      val stream = Language[P, F].classLoaderResourceStream(path)
      val map = Language[P, F].parseLanguageFile(stream)
      Logger[F].debug(s"Loading Java resources $path") *>
        map.flatMap {
          case Left(_) =>
            Logger[F].warn("$path is not a valid language file") *> Map.empty
              .pure[F]
          case Right(value) => value.pure[F]
        }
    }
    val totalMap: F[Map[String, String]] =
      namespaces.foldLeft(Map.empty.pure[F]) { (acc, namespace) =>
        for {
          acc <- acc
          next <- load(namespace, localeCode)
        } yield acc ++ next
      }
    totalMap.map { map =>
      Language[P, F].create(key => map.get(key))
    }
  }

  def fromServer[P[_], F[_]: Monad: Logger](
      server: P[MinecraftServer],
      localeCode: String,
  )(using Platform[P, F]): F[P[Language]] =
    MinecraftServer[P, F].resourceNamespaces.flatMap { namespaces =>
      fromJavaResource(namespaces, localeCode)
    }

  
}
