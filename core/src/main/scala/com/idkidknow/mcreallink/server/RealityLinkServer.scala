package com.idkidknow.mcreallink.server

import cats.Applicative
import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import io.netty.channel.ChannelPipeline
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.NettyConfig
import sttp.tapir.server.netty.cats.NettyCatsServer
import sttp.ws.WebSocketFrame

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
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def minecraftChatServerEndpoint[F[_]: Concurrent](
      interface: ChatInterface[F]
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

  def netty[F[_]: Async]: RealityLinkServer[F] =
    new RealityLinkServer[F] {
      def run(
          interface: ChatInterface[F],
          config: RealityLinkServerConfig,
      ): Resource[F, Unit] =
        Dispatcher
          .parallel[F]
          .flatMap { dispatcher =>
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

            Resource.make(
              NettyCatsServer[F](dispatcher)
                .config(nettyConfig)
                .addEndpoint(minecraftChatServerEndpoint(interface))
                .start()
            )(_.stop())
          }
          .map(_ => ())
    }
}
