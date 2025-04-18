package com.idkidknow.mcreallink.lib

import cats.Apply
import cats.effect.Concurrent
import cats.kernel.Monoid
import cats.syntax.all.*
import de.lhns.fs2.compress.Unarchiver
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.WalkOptions
import org.typelevel.log4cats.Logger

import java.io.IOException
import java.util.zip.ZipEntry
import com.idkidknow.mcreallink.api.Minecraft
import java.io.InputStream
import cats.effect.kernel.Async

opaque type LanguageMap = Map[String, String]

object LanguageMap {
  def apply(map: Map[String, String]): LanguageMap = map

  extension (map: LanguageMap) {
    def toFunction: String => Option[String] = { key => map.get(key) }
  }

  /** Noncommutative monoid. `combine(x, y)` prefer values in `x`. */
  given monoid: Monoid[LanguageMap] = new Monoid[LanguageMap] {
    override def empty: LanguageMap = Map.empty
    override def combine(x: LanguageMap, y: LanguageMap): LanguageMap = y ++ x
  }

  type LanguageFileParser[F[_]] =
    Stream[F, Byte] => F[Option[Map[String, String]]]

  object LanguageFileParser {
    def fromMinecraft[F[_]: Async](using
        mc: Minecraft
    ): LanguageFileParser[F] = { stream =>
      val jStream: Stream[F, InputStream] = stream.through(fs2.io.toInputStream)
      jStream
        .map(mc.Language.parseLanguageFile)
        .compile
        .onlyOrError
    }
  }

  /** Reads a language map from a zip archive (zip or jar, like resource packs
   *  or mod jars). Reads specified .json/.lang files in
   *  `assets/{namespace}/lang/` for all namespaces.
   *
   *  Ignores entries that errors occured and give a warning. Returns `None` if
   *  there's an [[IOException]] when unarchiving the zip.
   *
   *  @param filename
   *    locale code with an extension name, e.g. `en_us.json`, `en_US.lang`
   */
  def fromArchive[F[_]: Apply: Concurrent: Logger](
      stream: Stream[F, Byte],
      parser: LanguageFileParser[F],
      filename: String,
  )(using
      unarchiver: Unarchiver[F, Option, ZipEntry]
  ): F[Option[LanguageMap]] = {
    stream
      .through(unarchiver.unarchive)
      .flatMap { case (entry, data) =>
        val name = entry.name
        val regex = show"^assets/[^/]+/lang/$filename$$".r
        if (regex.matches(name)) {
          val parsed: F[Option[LanguageMap]] = parser(data)
          val languageMap: F[Stream[F, LanguageMap]] = parsed.map {
            case Some(map) => Stream.emit(map)
            case None =>
              Stream.exec(Logger[F].warn(show"failed to parse entry $name"))
          }
          Stream.eval(languageMap).flatten
        } else {
          data.drain
        }
      }
      .compile
      .foldMonoid(using monoid)
      .map(Some(_))
      .recoverWith { case e: IOException =>
        Logger[F].warn(e)("Error reading archive") *> None.pure[F]
      }
  }

  /** Read all .zip resource pack files and .jar mod files in the specified
   *  directory. See [[fromArchive]]
   *
   *  Returns empty map if IOException is thrown
   */
  def fromArchiveDirectory[F[_]: Apply: Concurrent: Logger: Files](
      directoryPath: Path,
      parser: LanguageFileParser[F],
      filename: String,
      maxDepth: Int,
  )(using
      unarchiver: Unarchiver[F, Option, ZipEntry]
  ): F[LanguageMap] = {
    def readZipFile(path: Path): F[LanguageMap] = {
      val stream = Files[F].readAll(path)
      fromArchive(stream, parser, filename).flatMap {
        case Some(map) => map.pure[F]
        case None =>
          Logger[F]
            .warn(show"failed to parse archive $path")
            .as(Map.empty)
      }
    }

    Logger[F].info(show"Finding language files in $directoryPath") *> Files[F]
      .walk(directoryPath, WalkOptions.Default.withMaxDepth(maxDepth))
      .filter(path => path.extName === ".zip" || path.extName === ".jar")
      .flatMap { path =>
        Stream.eval(readZipFile(path))
      }
      .compile
      .foldMonoid(using monoid)
      .recoverWith { case e: IOException =>
        Logger[F].warn(e)("Error reading archive directory") *>
          Map.empty.pure[F]
      }
  }
}
