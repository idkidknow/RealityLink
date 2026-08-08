package com.idkidknow.realitylink.server

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.Stream
import fs2.io.net.Network
import io.circe.parser
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.typelevel.log4cats.LoggerFactory

import scala.concurrent.duration.*

enum BroadcastRequest {
  case Literal(text: String)
  case Json(json: String)
}
object BroadcastRequest {
  import io.circe.derivation.Configuration
  import io.circe.derivation.ConfiguredCodec
  given Configuration = Configuration.default.withSnakeCaseConstructorNames
    .withDiscriminator("type")
  given ConfiguredCodec[BroadcastRequest] = ConfiguredCodec.derived
}

case class ChatEvent(json: String, translatedText: String)
object ChatEvent {
  given io.circe.Codec[ChatEvent] = io.circe.generic.semiauto.deriveCodec
}

trait RealityLinkServer[F[_]] {
  def run(
      interface: ChatInterface[F],
      config: RealityLinkServerConfig,
  ): Resource[F, Unit]
}

object RealityLinkServer {
  private def minecraftChatServerEndpoint[F[_]: {Async, LoggerFactory}](
      wsb: WebSocketBuilder2[F],
      interface: ChatInterface[F]
  ): HttpRoutes[F] = {
    val logger = LoggerFactory[F].getLogger
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "minecraft-chat" =>
        wsb.build { receive =>
          val broadcastInput: Stream[F, Nothing] =
            receive.flatMap {
              case WebSocketFrame.Text((text, _)) =>
                parser.decode[BroadcastRequest](text) match {
                  case Left(e) => Stream.exec(logger.warn(e)("Received invalid json"))
                  case Right(req) => Stream.exec(interface.broadcastInGame(req))
                }
              case _ =>
                Stream.exec(logger.warn("Received invalid message"))
            }

          val out: Stream[F, WebSocketFrame] =
            interface.outwardMessages.map { event =>
              WebSocketFrame.Text(event.asJson.noSpaces)
            }

          val autoPing: Stream[F, WebSocketFrame] =
            Stream.awakeEvery[F](20.seconds).as(WebSocketFrame.Ping())

          out.concurrently(broadcastInput).mergeHaltBoth(autoPing)
        }
    }
  }

  def apply[F[_]: {Async, LoggerFactory, Network}]: RealityLinkServer[F] = 
    (interface: ChatInterface[F], config: RealityLinkServerConfig) => {
      EmberServerBuilder.default[F]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpWebSocketApp(wsb => minecraftChatServerEndpoint(wsb, interface).orNotFound)
        .build
        .void
    }

}
