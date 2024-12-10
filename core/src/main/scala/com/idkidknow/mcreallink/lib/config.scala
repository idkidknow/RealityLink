package com.idkidknow.mcreallink.lib

import cats.data.EitherT
import cats.effect.implicits.*
import cats.effect.kernel.Async
import cats.syntax.all.*
import com.idkidknow.mcreallink.minecraft.Minecraft
import com.idkidknow.mcreallink.server.RealityLinkServerConfig
import com.idkidknow.mcreallink.server.TlsConfig
import com.idkidknow.mcreallink.common.LanguageMap
import com.idkidknow.mcreallink.utils.decodeToml
import de.lhns.fs2.compress.Unarchiver
import fs2.io.file.Files
import fs2.io.file.Path
import org.typelevel.log4cats.Logger

import java.io.IOException
import java.util.zip.ZipEntry
import cats.kernel.Monoid

final case class ModConfig(
    serverConfig: RealityLinkServerConfig,
    language: String => Option[String],
    autoStart: Boolean,
)

enum ConfigReadingException extends Exception {
  case IO(e: IOException)
  case Parsing(e: Exception)
  case Format(message: String)
}

final case class ServerToml(
    host: Option[String],
    port: Int,
    localeCode: String,
    resourcePackDirs: List[String],
    autoStart: Boolean,
    certChain: Option[String],
    privateKey: Option[String],
    root: Option[String],
)

object ModConfig {
  def fromServerToml[F[_]: Async: Logger: Files](using mc: Minecraft)(
      serverToml: ServerToml,
      server: mc.MinecraftServer,
  )(using
      Unarchiver[F, Option, ZipEntry]
  ): F[Either[ConfigReadingException, ModConfig]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]

    val gameRootDirectory = Path.fromNioPath(mc.gameRootDirectory)
    val configDirectory = Path.fromNioPath(mc.configDirectory)
    val certChain = serverToml.certChain.map(configDirectory.resolve)
    val privateKey = serverToml.privateKey.map(configDirectory.resolve)
    val root = serverToml.root.map(configDirectory.resolve(_))
    val resourcePackDirs =
      serverToml.resourcePackDirs.map(gameRootDirectory.resolve(_))

    val tlsConfig: FE[TlsConfig] =
      (certChain, privateKey, root) match {
        case (Some(certChain), Some(privateKey), None) =>
          TlsConfig
            .Tls(certChain, privateKey)
            .pure[FE]
        case (Some(certChain), Some(privateKey), Some(root)) =>
          TlsConfig
            .MutualTls(certChain, privateKey, root)
            .pure[FE]
        case (None, None, None) =>
          TlsConfig.None.pure[FE]
        case _ =>
          EitherT.leftT(
            ConfigReadingException.Format("Invalid server.toml tls config")
          )
      }

    def languageFileParser(
        stream: fs2.Stream[F, Byte]
    ): F[Option[Map[String, String]]] = {
      val jStream = stream.through(fs2.io.toInputStream)
      jStream
        .map { in =>
          mc.Language.parseLanguageFile(in)
        }
        .compile
        .onlyOrError
    }

    val language: FE[String => Option[String]] = EitherT.right {
      val fallback = LanguageMap(Map.empty).pure[F]
      val resourcePack = resourcePackDirs.map { dir =>
        LanguageMap.fromArchiveDirectory(
          dir,
          languageFileParser,
          show"${serverToml.localeCode}.lang",
          1,
        )
      }.sequence.map(Monoid[LanguageMap].combineAll(_))
      (fallback, resourcePack).parMapN { (fallback, resourcePack) =>
        fallback.combine(resourcePack).toFunction
      }
    }
    (tlsConfig, language).mapN { (tlsConfig, language) =>
      val realityLinkServerConfig =
        RealityLinkServerConfig(
          serverToml.host.getOrElse("0.0.0.0"),
          serverToml.port,
          tlsConfig,
        )
      ModConfig(realityLinkServerConfig, language, serverToml.autoStart)
    }.value
  }

  def fromConfigFile[F[_]: Async: Logger: Files](using mc: Minecraft)(
      server: mc.MinecraftServer
  )(using
      Unarchiver[F, Option, ZipEntry]
  ): F[Either[ConfigReadingException, ModConfig]] = {
    type FE[A] = EitherT[F, ConfigReadingException, A]

    val serverTomlPath =
      Path.fromNioPath(mc.configDirectory) / "reallink" / "server.toml"
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

    import io.circe.generic.auto.*
    serverTomlString.flatMap { str =>
      decodeToml[ServerToml](str) match {
        case Right(serverToml) =>
          EitherT(ModConfig.fromServerToml(serverToml, server))
        case Left(e) =>
          EitherT.leftT(ConfigReadingException.Parsing(e))
      }
    }.value
  }
}
