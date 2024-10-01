package com.idkidknow.mcrealcomm.api.server

import com.idkidknow.mcrealcomm.event.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.event.EventManager
import com.idkidknow.mcrealcomm.event.EventManagerProxy
import com.idkidknow.mcrealcomm.event.register
import com.idkidknow.mcrealcomm.l10n.ServerTranslate
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
import io.ktor.server.netty.Netty
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.Frame
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
    broadcastingMessageEventManager: EventManager<BroadcastingMessageEvent>,
    val config: ApiServerConfig,
    minecraftServer: MinecraftServer,
) {
    val broadcastingMessage = EventManagerProxy<BroadcastingMessageEvent>(broadcastingMessageEventManager)
    val job: Job

    init {
        suspend fun init(): EmbeddedServer<*, *> = coroutineScope {
            logger.info { "port: ${config.port}" }
            embeddedServer(Netty, port = config.port) {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json)
                }
                routing {
                    webSocket("/minecraft-chat") {
                        // Observe in-game chat
                        val scope = CoroutineScope(SupervisorJob() + this@coroutineScope.coroutineContext)
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
                            minecraftServer.playerList.broadcastSystemMessage(component, false)
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
