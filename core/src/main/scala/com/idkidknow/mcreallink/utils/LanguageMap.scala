package com.idkidknow.mcreallink.utils

import cats.Monoid
import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import de.lhns.fs2.compress.Unarchiver
import fs2.Pipe
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.WalkOptions
import org.typelevel.log4cats.Logger
import com.idkidknow.mcreallink.minecraft.Minecraft

import java.io.IOException

opaque type LanguageMap = Map[String, String]

object LanguageMap {
  def apply(map: Map[String, String]): LanguageMap = map

  extension (self: LanguageMap) {
    def toFunction: String => Option[String] = { key => self.get(key) }
  }

  /** Noncommutative monoid. `combine(x, y)` prefer values in `y`. */
  given monoid: Monoid[LanguageMap] = new Monoid[LanguageMap] {
    override def empty: LanguageMap = Map.empty
    override def combine(x: LanguageMap, y: LanguageMap): LanguageMap = x ++ y
  }

  /** Returns empty when failed */
  private def loadLanguageFile[F[_]: Async](
      stream: Stream[F, Byte],
      onFailure: F[Unit],
  )(using mc: Minecraft): F[LanguageMap] = for {
    inputStream <- stream.through(fs2.io.toInputStream).compile.onlyOrError
    map <- Async[F]
      .blocking(mc.Language.parseLanguageFile(inputStream))
      .flatMap {
        case Some(map) => map.pure[F]
        case None => onFailure *> Map.empty.pure[F]
      }
  } yield map

  /** Read language files in Java resources
   *
   *  Under normal circumstances, there are Minecraft's en_us.json, and language
   *  jsons in every mods' jar file
   */
  def fromJavaResource[F[_]: Async: Logger](
      namespaces: Iterable[String],
      localeCode: String,
  )(using mc: Minecraft): F[LanguageMap] = {
    def load(namespace: String, localeCode: String, ext: String): F[LanguageMap] = {
      val path = show"/assets/$namespace/lang/$localeCode.$ext"
      val stream = fs2.io.readInputStream(
        Async[F].delay(mc.Language.classLoaderResourceStream(path)),
        4096,
      )
      Logger[F].debug(show"Loading Java resources $path") *>
        loadLanguageFile(
          stream,
          onFailure = Logger[F].warn(show"$path is not a valid language file"),
        )
    }
    // namespaces.map(load(_, localeCode, "json")).toList.parFoldMapA(identity)
    namespaces.map(load(_, localeCode, "lang")).toList.parFoldMapA(identity)
  }

  def fromServer[F[_]: Async: Logger](using mc: Minecraft)(
      server: mc.MinecraftServer,
      localeCode: String,
  ): F[LanguageMap] =
    Async[F].delay(mc.MinecraftServer.resourceNamespaces(server)).flatMap {
      namespaces =>
        fromJavaResource(namespaces, localeCode)
    }

  def fromResourcePack[F[_]: Async: Logger](
      stream: Stream[F, Byte],
      localeCode: String,
  )(using
      mc: Minecraft,
      U: Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    stream
      .through(U.unarchive)
      .flatMap { case (entry, data) =>
        val name = entry.name
        // val regex = show"^assets/[^/]+/lang/$localeCode.json$$".r
        val regex = show"^assets/[^/]+/lang/$localeCode.lang$$".r
        if (regex.matches(name)) {
          val fLanguageMap = loadLanguageFile(
            data,
            onFailure = Logger[F].warn(show"$name is not a valid language file"),
          )
          Stream.eval(Logger[F].info(show"Loading entry: $name") *> fLanguageMap)
        } else {
          Stream.empty
        }
      }
      .compile
      .fold(Map.empty[String, String]) { case (acc, value) =>
        acc ++ value
      }
  }

  def fromResourcePackFile[F[_]: Async: Logger: Files](
      path: Path,
      localeCode: String,
  )(using
      mc: Minecraft,
      U: Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    val stream = Files[F].readAll(path)
    Logger[F].info(show"Loading resource pack $path") *>
      fromResourcePack(stream, localeCode)
  }

  def fromResourcePackDirectory[F[_]: Async: Logger: Files](
      directoryPath: Path,
      localeCode: String,
  )(using
      mc: Minecraft,
      U: Unarchiver[F, Option, ?],
  ): F[LanguageMap] = {
    def readFile: Pipe[F, Path, LanguageMap] = { paths =>
      paths.flatMap { path =>
        if (path.extName === ".zip") {
          Stream.eval(
            fromResourcePackFile(path, localeCode).recoverWith {
              case e: IOException =>
                Logger[F].warn(e)(show"Failed to read $path. Skipping.") *>
                  Map.empty[String, String].pure[F]
            }
          )
        } else {
          Stream.empty
        }
      }
    }
    Files[F]
      .walk(directoryPath, WalkOptions.Default.withMaxDepth(1))
      .through(readFile)
      .compile
      .fold(Map.empty[String, String]) { (acc, value) => acc ++ value }
  }
}
