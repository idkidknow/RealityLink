package com.idkidknow.mcreallink.lib

import cats.MonadThrow
import cats.effect.Concurrent
import cats.effect.implicits.*
import cats.syntax.all.*
import de.lhns.fs2.compress.ArchiveEntry
import de.lhns.fs2.compress.Archiver
import fs2.io.file.Files
import fs2.io.file.Path
import io.circe.Json
import org.http4s.Request
import org.http4s.Uri
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.syntax.all.*

import java.io.IOException

object AssetDownload {
  val VersionManifestUrl: Uri =
    uri"https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
  val ResourcesUrl = uri"https://resources.download.minecraft.net"

  enum DownloadException(msg: String) extends Exception(msg) {
    case IO(e: IOException)
        extends DownloadException(
          show"${e.getClass.getName}: ${e.getMessage()}"
        )
    case Decode(msg: String) extends DownloadException(msg)
    case VersionNotFound(version: String)
        extends DownloadException(show"version not found: $version")
  }
  type Result[A] = Either[DownloadException, A]
  extension [F[_]: MonadThrow, A](fa: F[A]) {
    def errorAdapted: F[A] = fa.adaptError {
      case e: IOException => DownloadException.IO(e)
      case e: org.http4s.DecodeFailure => DownloadException.Decode(e.getMessage)
      case e: io.circe.DecodingFailure => DownloadException.Decode(e.getMessage)
      case e: org.http4s.ParseFailure => DownloadException.Decode(e.getMessage)
    }
  }

  private def getVersionJsonUrl[F[_]: Concurrent](
      version: String,
      client: Client[F],
  ): F[Result[Uri]] = {
    val url: F[Uri] = for {
      json <- client.expect[Json](VersionManifestUrl)
      versions: Iterable[Json] <- json.hcursor
        .downField("versions")
        .values
        .toRight(
          DownloadException.Decode(
            show"Missing \"versions\" field in $VersionManifestUrl"
          )
        )
        .pure[F]
        .rethrow
      matched: Json <- versions
        .find { obj =>
          obj.hcursor.get[String]("id") === Right(version)
        }
        .toRight(DownloadException.VersionNotFound(version))
        .pure[F]
        .rethrow
      urlStr <- matched.hcursor.get[String]("url").pure[F].rethrow
      url: Uri <- Uri.fromString(urlStr).pure[F].rethrow
    } yield url
    url.errorAdapted.attemptNarrow[DownloadException]
  }

  private def getAssetIndexUrl[F[_]: Concurrent](
      pistonMetaJsonUrl: Uri,
      client: Client[F],
  ): F[Result[Uri]] = {
    val url: F[Uri] = for {
      json: Json <- client.expect[Json](pistonMetaJsonUrl)
      urlStr <- json.hcursor
        .downField("assetIndex")
        .downField("url")
        .as[String]
        .pure[F]
        .rethrow
      url: Uri <- Uri.fromString(urlStr).pure[F].rethrow
    } yield url
    url.errorAdapted.attemptNarrow[DownloadException]
  }

  private final case class LanguageObject(name: String, hash: String)

  private def getLanguageObjects[F[_]: Concurrent](
      assetIndexUrl: Uri,
      client: Client[F],
  ): F[Result[List[LanguageObject]]] = {
    val list: F[List[LanguageObject]] = for {
      json: Json <- client.expect[Json](assetIndexUrl)
      objects: List[(String, Json)] <- json.hcursor
        .downField("objects")
        .focus
        .flatMap(_.asObject)
        .map(_.toList)
        .toRight(
          DownloadException.Decode(
            show"Missing or with invalid \"objects\" field in $assetIndexUrl"
          )
        )
        .pure[F]
        .rethrow
    } yield objects.mapFilter { case (k, v) =>
      if (!k.startsWith("minecraft/lang/")) None
      else {
        v.asObject.flatMap(_.apply("hash")).flatMap(_.asString).map { hash =>
          LanguageObject(k, hash)
        }
      }
    }
    list.errorAdapted.attemptNarrow[DownloadException]
  }

  def downloadLanguageAssets[F[_]: Concurrent: Files](
      version: String,
      targetZipFilePath: Path,
      client: Client[F],
  )(using archiver: Archiver[F, Option]): F[Result[Unit]] = {
    val objs: F[List[LanguageObject]] = for {
      url1 <- getVersionJsonUrl(version, client).rethrow
      url2 <- getAssetIndexUrl(url1, client).rethrow
      objects <- getLanguageObjects(url2, client).rethrow
    } yield objects

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def downloadObject(obj: LanguageObject, dir: Path): F[Unit] = {
      val url: Uri = ResourcesUrl / obj.hash.substring(0, 2) / obj.hash
      val root: Path = dir.normalize
      val target: Path = root.resolve(obj.name).normalize
      val checkPath: F[Unit] =
        if (!target.startsWith(root))
          MonadThrow[F].raiseError(
            DownloadException.Decode(show"Invalid object name ${obj.name}")
          )
        else ().pure[F]
      val createDir: F[Unit] =
        Files[F].createDirectories(target.parent.getOrElse(dir))
      val byteStream = client.stream(Request[F](uri = url)).flatMap(_.body)
      val writeFile: F[Unit] = byteStream
        .through(Files[F].writeAll(target))
        .compile
        .drain
      checkPath >> createDir >> writeFile
    }

    val createTempDir = Files[F].createTempDirectory

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def createArchive(dir: Path): F[Unit] = {
      val assetDir = dir / "assets"
      Files[F]
        .walk(assetDir)
        .evalFilter(Files[F].isRegularFile(_))
        .evalMap { file =>
          val name = dir.relativize(file).toString.replace('\\', '/')
          Files[F].size(file).map { size =>
            ArchiveEntry(name, Some(size)) -> Files[F].readAll(file)
          }
        }
        .through(archiver.archive)
        .through(Files[F].writeAll(targetZipFilePath))
        .compile
        .drain
    }

    val product: F[Unit] = (objs, createTempDir).flatMapN { (objs, dir) =>
      Files[F].createDirectories(dir / "assets") >>
        objs.parTraverse(downloadObject(_, dir / "assets")) >>
        createArchive(dir)
    }

    product.errorAdapted.attemptNarrow[DownloadException]
  }
}
