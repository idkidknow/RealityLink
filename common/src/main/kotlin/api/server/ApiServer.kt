package com.idkidknow.mcrealcomm.api.server

import com.idkidknow.mcrealcomm.event.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.event.SetUnitEventManagerProxy
import com.idkidknow.mcrealcomm.event.UnitEventManager
import com.idkidknow.mcrealcomm.event.noBroadCastingMessageEventCurrentThread
import com.idkidknow.mcrealcomm.event.register
import com.idkidknow.mcrealcomm.l10n.ServerTranslate
import com.idkidknow.mcrealcomm.l10n.logger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.webSocket
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.Frame
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

private val logger = KotlinLogging.logger {}

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

class ApiServer(
    broadcastingMessageEventManager: UnitEventManager<BroadcastingMessageEvent>,
    val config: ApiServerConfig,
    minecraftServer: MinecraftServer,
) {
    val broadcastingMessage = SetUnitEventManagerProxy<BroadcastingMessageEvent>(broadcastingMessageEventManager)
    val job: Job

    init {
        suspend fun init(): EmbeddedServer<*, *> = coroutineScope {
            logger.info { "port: ${config.port}" }
            val configure: NettyApplicationEngine.Configuration.() -> Unit = configure@{
                connector {
                    port = config.port
                }

                val sslContext = when (config.tlsConfig) {
                    is TlsConfig.None -> {
                        return@configure
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

            embeddedServer(Netty, applicationEnvironment(), configure) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json)
                }
                routing {
                    webSocket("/minecraft-chat") {
                        // Observe in-game chat
                        val scope = CoroutineScope(coroutineContext + SupervisorJob())
                        val handler = broadcastingMessage.register { (component) ->
                            val response = component.toChatResponse(minecraftServer, config.language)
                            logger.debug { "sending response: $response" }
                            scope.launch {
                                sendSerialized(response)
                            }
                        }
                        // Handle incoming
                        for (frame in incoming) {
                            val component = try {
                                 frame.toComponent(minecraftServer, converter!!)
                            } catch (_: Exception) {
                                continue
                            }
                            minecraftServer.playerList.noBroadCastingMessageEventCurrentThread {
                                broadcastSystemMessage(component, false)
                            }
                        }
                        handler.unregister()
                    }
                }
            }.start(wait = false)
        }

        job = CoroutineScope(Dispatchers.IO + Job()).launch {
            init()
        }
    }

    fun stop() {
        job.cancel()
        broadcastingMessage.removeProxy()
    }
}

private fun Component.toChatResponse(minecraftServer: MinecraftServer, language: Language): ChatResponse {
    val json = Component.Serializer.toJson(this, minecraftServer.registryAccess())
    val translatedText = ServerTranslate.translate(this, language)
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
