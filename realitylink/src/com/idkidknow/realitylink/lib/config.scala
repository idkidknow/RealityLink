package com.idkidknow.realitylink.lib

import cats.data.EitherT
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.effect.kernel.Concurrent
import cats.kernel.Monoid
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.idkidknow.realitylink.lib.LanguageMap.LanguageFileParser
import com.idkidknow.realitylink.lib.decodeToml
import com.idkidknow.realitylink.platform
import com.idkidknow.realitylink.server.RealityLinkServerConfig
import de.lhns.fs2.compress.Unarchiver
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.text
import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import org.typelevel.log4cats.Logger

import java.io.IOException
import java.util.zip.ZipEntry

final case class ServerToml(
    host: Option[Host],
    port: Port,
    localeCode: String,
    resourcePackDirs: List[String],
    autoStart: Boolean,
)

object ServerToml {
  given Codec[Host] = Codec.from(
    decodeA = Decoder.decodeString.emap { str =>
      Host.fromString(str).toRight("invalid host")
    },
    encodeA = Encoder.encodeString.contramap { h =>
      h.toString
    },
  )
  given Codec[Port] = Codec.from(
    decodeA = Decoder.decodeInt.emap { i =>
      Port.fromInt(i).toRight("invalid port")
    },
    encodeA = Encoder.encodeInt.contramap { p =>
      p.value
    },
  )
  given Codec[ServerToml] = Codec.derived

  def writeDefault[F[_]: {Concurrent, Files}](path: Path): F[Unit] = {
    Stream
      .emit(defaultTomlString)
      .through(text.utf8.encode)
      .through(Files[F].writeAll(path))
      .compile
      .drain
  }
  private def defaultTomlString: String =
    """host = "0.0.0.0"
      |port = 39244
      |localeCode = "en_us"
      |resourcePackDirs = ["mod", "serverlang"]
      |autoStart = false
      |""".stripMargin
}

final case class ModConfig(
    serverConfig: RealityLinkServerConfig,
    language: String => Option[String],
    autoStart: Boolean,
)

object ModConfig {
  enum ConfigReadingException extends Exception {
    case IO(e: IOException)
    case Parsing(e: Exception)
  }

  private def fromServerToml[F[_]: {Concurrent, Logger, Files}](
      serverToml: ServerToml,
      gameRootDirectory: Path,
      languageFileExtension: String,
      languageFileParser: LanguageFileParser[F],
  )(using
      Unarchiver[F, Option, ZipEntry]
  ): F[Either[ConfigReadingException, ModConfig]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]
    val resourcePackDirs =
      serverToml.resourcePackDirs.map(gameRootDirectory.resolve)

    val language: FE[String => Option[String]] = EitherT.right {
      val resourcePack = resourcePackDirs
        .map { dir =>
          LanguageMap.fromArchiveDirectory(
            dir,
            languageFileParser,
            show"${serverToml.localeCode}.$languageFileExtension",
            1,
          )
        }
        .parSequence
        .map(Monoid[LanguageMap].combineAll(_))
      resourcePack.map(_.toFunction)
    }
    language.map { language =>
      val realityLinkServerConfig =
        RealityLinkServerConfig(
          serverToml.host.getOrElse(host"0.0.0.0"),
          serverToml.port,
        )
      ModConfig(realityLinkServerConfig, language, serverToml.autoStart)
    }.value
  }

  def fromConfigFile[F[_]: {Async, Logger, Files}](using
      unarchiver: Unarchiver[F, Option, ZipEntry]
  ): F[Either[ConfigReadingException, ModConfig]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]

    val serverTomlPath =
      platform.configDirectory / "realitylink" / "server.toml"
    val serverTomlString: FE[String] = EitherT(
      Files[F]
        .readUtf8(serverTomlPath)
        .compile
        .string
        .attemptNarrow[IOException]
        .map { ioEither =>
          ioEither.leftMap(e => ConfigReadingException.IO(e))
        }
    )

    serverTomlString.flatMap { str =>
      decodeToml[ServerToml](str) match {
        case Right(serverToml) =>
          EitherT(
            ModConfig.fromServerToml(
              serverToml,
              platform.gameRootDirectory,
              platform.Language.languageFileExtension,
              LanguageFileParser[F],
            )
          )
        case Left(e) =>
          EitherT.leftT(ConfigReadingException.Parsing(e))
      }
    }.value
  }
}
