package com.idkidknow.mcrealcomm.utils

import cats.Monad
import cats.Monoid
import cats.effect.Concurrent
import cats.effect.implicits.*
import cats.syntax.all.*
import com.idkidknow.mcreallink.lib.platform.Language
import com.idkidknow.mcreallink.lib.platform.MinecraftServer
import com.idkidknow.mcreallink.lib.platform.Platform
import de.lhns.fs2.compress.Unarchiver
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.WalkOptions
import org.typelevel.log4cats.Logger

import java.io.IOException

opaque type LanguageMap = Map[String, String]

object LanguageMap {
  def apply(map: Map[String, String]): LanguageMap = map

  extension (self: LanguageMap) {
    def toLanguage[P[_], F[_]](using Platform[P, F]): P[Language] =
      Language[P, F].create(key => self.get(key))
  }

  /** Noncommutative monoid. `combine(x, y)` prefer values in `y`. */
  given monoid: Monoid[LanguageMap] = new Monoid[LanguageMap] {
    override def empty: LanguageMap = Map.empty
    override def combine(x: LanguageMap, y: LanguageMap): LanguageMap = x ++ y
  }

  private def loadLanguageFile[P[_], F[_]: Monad](
      stream: Stream[F, Byte],
      onFailure: F[Unit],
  )(using Platform[P, F]): F[LanguageMap] = {
    Language[P, F].parseLanguageFile(stream).flatMap {
      case Left(_)      => onFailure *> Map.empty.pure[F]
      case Right(value) => value.pure[F]
    }
  }

  /** Read language files in Java resources
   *
   *  Under normal circumstances, there are Minecraft's en_us.json, and language
   *  jsons in every mods' jar file
   */
  def fromJavaResource[P[_], F[_]: Concurrent: Logger](
      namespaces: Iterable[String],
      localeCode: String,
  )(using Platform[P, F]): F[LanguageMap] = {
    def load(namespace: String, localeCode: String): F[LanguageMap] = {
      val path = s"/assets/$namespace/lang/$localeCode.json"
      val stream = Language[P, F].classLoaderResourceStream(path)
      Logger[F].debug(s"Loading Java resources $path") *>
        loadLanguageFile(
          stream,
          onFailure = Logger[F].warn(s"$path is not a valid language file"),
        )
    }
    namespaces.map(load(_, localeCode)).toList.parFoldMapA(identity)
  }

  def fromServer[P[_], F[_]: Concurrent: Logger](
      server: P[MinecraftServer],
      localeCode: String,
  )(using Platform[P, F]): F[LanguageMap] =
    MinecraftServer[P, F].resourceNamespaces.flatMap { namespaces =>
      fromJavaResource(namespaces, localeCode)
    }

  def fromResourcePack[P[_], F[_]: Concurrent: Logger](
      stream: Stream[F, Byte],
      localeCode: String,
  )(using
      p: Platform[P, F],
      unarchiver: Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    stream
      .through(unarchiver.unarchive)
      .flatMap { (entry, data) =>
        val name = entry.name
        val regex = s"^assets/[^/]+/lang/$localeCode.json$$".r
        if (regex.matches(name)) {
          val fLanguageMap = loadLanguageFile(
            data,
            onFailure = Logger[F].warn(s"$name is not a valid language file"),
          )
          Stream.eval(fLanguageMap)
        } else {
          Stream.empty
        }
      }
      .compile
      .fold(Map.empty[String, String]) { (acc, value) => acc ++ value }
  }

  def fromResourcePackFile[P[_], F[_]: Concurrent: Logger: Files](
      path: Path,
      localeCode: String,
  )(using
      Platform[P, F],
      Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    val stream = Files[F].readAll(path)
    fromResourcePack(stream, localeCode)
  }

  def fromResourcePackDirectory[P[_], F[_]: Concurrent: Logger: Files](
      directoryPath: Path,
      localeCode: String,
  )(using
      Platform[P, F],
      Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    def readFile: Pipe[F, Path, LanguageMap] = { paths =>
      paths.flatMap { path =>
        if (path.extName === "zip") {
          Stream.eval(
            fromResourcePackFile(path, localeCode).recoverWith {
              case e: IOException =>
                Logger[F].warn(e)(s"Failed to read $path. Skipping.") *>
                  Map.empty[String, String].pure[F]
            }
          )
        } else {
          Stream.empty
        }
      }
    }
    Files[F].walk(directoryPath, WalkOptions.Default.withMaxDepth(1))
      .through(readFile)
      .compile
      .fold(Map.empty[String, String]) { (acc, value) => acc ++ value }
  }
}