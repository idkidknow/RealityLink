package com.idkidknow.mcreallink.server

import cats.effect.Concurrent
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.capabilities.WebSockets
import sttp.tapir.server.ServerEndpoint
import cats.Applicative
import sttp.ws.WebSocketFrame
import scala.concurrent.duration.*
import sttp.tapir.server.netty.cats.NettyCatsServer
import cats.effect.std.Dispatcher
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.channel.ChannelPipeline
import io.netty.handler.ssl.SslHandler
import sttp.tapir.server.netty.NettyConfig

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

@SuppressWarnings(Array("org.wartremover.warts.Any"))
def minecraftChatServerEndpoint[F[_]: Concurrent](
    interface: MinecraftInterface[F]
): ServerEndpoint[Fs2Streams[F] & WebSockets, F] = {
  endpoint.get
    .in("minecraft-chat")
    .out(
      webSocketBody[
        BroadcastRequest,
        CodecFormat.Json,
        ChatEvent,
        CodecFormat.Json,
      ](Fs2Streams[F])
        .ignorePong(true)
        .autoPongOnPing(true)
        .autoPing(Some((20.seconds, WebSocketFrame.ping)))
    )
    .serverLogicSuccess[F] { _ =>
      Applicative[F].pure { in =>
        val broadcastInput: fs2.Stream[F, Unit] =
          in.evalMap(interface.broadcastInGame(_))
        val out: fs2.Stream[F, ChatEvent] = interface.outwardMessages
        out.concurrently(broadcastInput)
      }
    }
}

trait RealityLinkServer[F[_]] {
  type Server
  def create(
      interface: MinecraftInterface[F],
      config: RealityLinkServerConfig,
  ): Resource[F, Server]
}

object RealityLinkServer {
  def netty[F[_]: Async]: RealityLinkServer[F] = new RealityLinkServer[F] {
    type Server = NettyCatsServer[F]
    def create(
        interface: MinecraftInterface[F],
        config: RealityLinkServerConfig,
    ): Resource[F, Server] =
      Dispatcher.parallel[F].map { dispatcher =>
        val sslContext: Option[SslContext] = config.tlsConfig match {
          case TlsConfig.None => None
          case TlsConfig.Tls(certChain, privateKey) =>
            Some(
              SslContextBuilder
                .forServer(
                  certChain.toNioPath.toFile,
                  privateKey.toNioPath.toFile,
                )
                .build()
            )
          case TlsConfig.MutualTls(certChain, privateKey, root) =>
            Some(
              SslContextBuilder
                .forServer(
                  certChain.toNioPath.toFile,
                  privateKey.toNioPath.toFile,
                )
                .trustManager(root.toNioPath.toFile)
                .build()
            )
        }

        def sslHandler(pipeline: ChannelPipeline): Option[SslHandler] =
          sslContext.map { sslContext =>
            val engine = sslContext.newEngine(pipeline.channel().alloc())
            engine.setUseClientMode(false)
            config.tlsConfig match {
              case TlsConfig.MutualTls(_, _, _) =>
                engine.setNeedClientAuth(true)
              case _ => engine.setNeedClientAuth(false)
            }
            SslHandler(engine)
          }

        val nettyConfig: NettyConfig = NettyConfig.default
          .host(config.host)
          .port(config.port)
          .initPipeline { cfg =>
            { (pipeline, handler) =>
              sslHandler(pipeline).foreach(pipeline.addFirst(_))
              NettyConfig.defaultInitPipeline(cfg)(pipeline, handler)
            }
          }

        NettyCatsServer[F](dispatcher)
          .config(nettyConfig)
          .addEndpoint(minecraftChatServerEndpoint(interface))
      }
  }
}
