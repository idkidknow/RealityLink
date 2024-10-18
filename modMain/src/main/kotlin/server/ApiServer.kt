package com.idkidknow.mcreallink.server

import com.idkidknow.mcreallink.api.UnitCallbackRegistry
import com.idkidknow.mcreallink.api.register
import com.idkidknow.mcreallink.context.ModContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.serialization.deserialize
import io.ktor.server.plugins.origin
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.websocket.close
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

interface ApiServer {
    fun start()
    fun stop()
}

@Serializable
private sealed interface ChatRequest {
    @Serializable
    @SerialName("literal")
    data class Literal(val text: String) : ChatRequest
    @Serializable
    @SerialName("json")
    data class Json(val json: String) : ChatRequest
}
@Serializable
private data class ChatResponse(val json: String, val translatedText: String)

fun ModContext.createApiServer(config: ApiServerConfig): ApiServer = object : ApiServer {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var server: ApiServer? = null

    override fun start() {
        server = scope.createApiServer(this@createApiServer, config)
        server!!.start()
    }

    override fun stop() {
        thread {
            server?.stop()
            scope.cancel()
        }
    }

}

fun CoroutineScope.createApiServer(
    ctx: ModContext,
    config: ApiServerConfig,
): ApiServer = object : ApiServer {
    var server: EmbeddedServer<*, *>? = null

    override fun start() {
        server = embeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>(
            factory = Netty,
            rootConfig = serverConfig {
                module { module() }
                parentCoroutineContext = coroutineContext
            },
            configure = { transportLayerConfiguration() }
        ).start(wait = false)
    }

    override fun stop() {
        server?.stop()
        server = null
    }

    private fun Application.module() {
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 10.seconds
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        routing {
            webSocket("/minecraft-chat") {
                logger.info { "connected: ${this@webSocket.call.request.origin.remoteHost}" }

                // Observe in-game chat
                val scope = CoroutineScope(coroutineContext + SupervisorJob())
                val handler = ctx.modEvents.broadcastingMessage.register { (component) ->
                    val response = component.toChatResponse(ctx, config.language)
                    logger.debug { "sending response: $response" }
                    scope.launch {
                        sendSerialized(response)
                    }
                }

                try {
                    // Handle incoming
                    for (frame in incoming) {
                        val component = try {
                            frame.toComponent(ctx.minecraftServer, converter!!)
                        } catch (_: Exception) {
                            continue
                        }
                        ctx.broadcastMessageWithoutCallback(ctx.minecraftServer.playerList, component)
                    }
                } finally {
                    scope.cancel()
                    handler.unregister()
                    this@webSocket.close()
                    logger.info { "disconnected: ${this@webSocket.call.request.origin.remoteHost}" }
                }
            }
        }
    }

    private fun NettyApplicationEngine.Configuration.transportLayerConfiguration() {
        connector {
            port = config.port
        }

        val sslContext = when (config.tlsConfig) {
            is TlsConfig.None -> {
                return
            }
            is TlsConfig.Tls -> {
                val (certChain, privateKey) = config.tlsConfig
                SslContextBuilder.forServer(certChain, privateKey).build()
            }
            is TlsConfig.MutualTls -> {
                val (certChain, privateKey, root) = config.tlsConfig
                SslContextBuilder.forServer(certChain, privateKey).trustManager(root).build()
            }
        }

        // configure Netty's SSL
        channelPipelineConfig = {
            val engine = sslContext.newEngine(channel().alloc()).apply {
                useClientMode = false
                if (config.tlsConfig is TlsConfig.MutualTls) {
                    needClientAuth = true
                }
            }
            addFirst(SslHandler(engine))
        }
    }
}

private fun Component.toChatResponse(ctx: ModContext, language: Language): ChatResponse {
    val json = Component.Serializer.toJson(this, ctx.minecraftServer.registryAccess())
    val translatedText = ctx.translate(this, language)
    return ChatResponse(json, translatedText)
}

private suspend fun Frame.toComponent(minecraftServer: MinecraftServer, converter: WebsocketContentConverter): Component {
    val request = converter.deserialize<ChatRequest>(this@toComponent)
    return when (request) {
        is ChatRequest.Literal -> Component.literal(request.text)
        is ChatRequest.Json -> try {
            Component.Serializer.fromJson(request.json, minecraftServer.registryAccess())!!
        } catch (_: Exception) {
            // Send the raw json text
            Component.literal(request.json)
        }
    }
}
