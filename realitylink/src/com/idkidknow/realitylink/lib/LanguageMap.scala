package com.idkidknow.realitylink.lib

import cats.effect.Resource
import cats.effect.Sync
import cats.effect.kernel.Async
import cats.kernel.Monoid
import cats.syntax.all.*
import com.idkidknow.realitylink.platform.Language
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.io.file.WalkOptions
import org.typelevel.log4cats.Logger

import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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
    def apply[F[_]: Async]: LanguageFileParser[F] = { stream =>
      val jStream: Stream[F, InputStream] = stream.through(fs2.io.toInputStream)
      jStream
        .evalMap(input => Async[F].blocking(Language.parseLanguageFile(input)))
        .compile
        .onlyOrError
    }
  }

  /** Reads a language map from a zip archive (zip or jar, like resource packs
   *  or mod jars). Reads specified .json/.lang files in
   *  `assets/{namespace}/lang/` for all namespaces.
   *
   *  Ignores entries that errors occurred and give a warning. Returns `None` if
   *  there's an [[IOException]] when unarchiving the zip.
   *
   *  @param filename
   *    locale code with an extension name, e.g. `en_us.json`, `en_US.lang`
   */
  private def fromArchiveFile[F[_]: {Async, Logger}](
      archivePath: Path,
      parser: LanguageFileParser[F],
      filename: String,
  ): F[Option[LanguageMap]] = {
    import scala.jdk.CollectionConverters.*
    Resource
      .fromAutoCloseable(
        Sync[F].blocking(ZipFile(archivePath.toNioPath.toFile))
      )
      .use { zipFile =>
        val entries: F[LazyList[ZipEntry]] =
          Sync[F].blocking(zipFile.entries()).map { entries =>
            entries.asScala
              .filter { entry =>
                val parts = entry.getName.split('/')
                !entry.isDirectory
                && parts.length === 4
                && parts(0) === "assets"
                && parts(2) === "lang"
                && parts(3) === filename
              }
              .to(LazyList)
          }
        Stream
          .evalSeq(entries)
          .flatMap { entry =>
            val data = fs2.io.readInputStream(
              fis = Sync[F].blocking(zipFile.getInputStream(entry)),
              chunkSize = 8192,
              closeAfterUse = true,
            )
            val parsed: F[Option[LanguageMap]] = parser(data)
            val languageMap: F[Stream[F, LanguageMap]] = parsed.map {
              case Some(map) => Stream.emit(map)
              case None =>
                Stream.exec(
                  Logger[F].warn(show"failed to parse entry ${entry.getName}")
                )
            }
            Stream.eval(languageMap).flatten
          }
          .compile
          .foldMonoid(using monoid)
          .map(Some(_))
      }
      .recoverWith { case e: IOException =>
        Logger[F].warn(e)("Error reading archive") *> None.pure[F]
      }
  }

  /** Read all .zip resource pack files and .jar mod files in the specified
   *  directory. See [[fromArchive]]
   *
   *  Returns empty map if IOException is thrown
   */
  def fromArchiveDirectory[F[_]: {Async, Logger, Files}](
      directoryPath: Path,
      parser: LanguageFileParser[F],
      filename: String,
      maxDepth: Int,
  ): F[LanguageMap] = {
    def readZipFile(path: Path): F[LanguageMap] = {
      fromArchiveFile(path, parser, filename).flatMap {
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
