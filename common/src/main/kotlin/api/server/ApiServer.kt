package com.idkidknow.mcrealcomm.api.server

import com.idkidknow.mcrealcomm.event.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.event.RegisteredEventHandler
import com.idkidknow.mcrealcomm.event.SetUnitEventManagerProxy
import com.idkidknow.mcrealcomm.event.UnitEventManager
import com.idkidknow.mcrealcomm.event.noBroadCastingMessageEventCurrentThread
import com.idkidknow.mcrealcomm.event.register
import com.idkidknow.mcrealcomm.l10n.ServerTranslate
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import io.javalin.community.ssl.SslPlugin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    val app: Javalin = Javalin.create config@ { javalinConfig ->
        if (config.tlsConfig is TlsConfig.None) {
            return@config
        }
        val sslPlugin = SslPlugin { conf -> conf.run {
            insecure = false
            securePort = config.port
            when (config.tlsConfig) {
                is TlsConfig.MutualTls -> {
                    pemFromPath(config.tlsConfig.certChain.absolutePath, config.tlsConfig.privateKey.absolutePath)
                    withTrustConfig { trust ->
                        trust.certificateFromPath(config.tlsConfig.root.absolutePath)
                    }
                }
                is TlsConfig.Tls -> {
                    pemFromPath(config.tlsConfig.certChain.absolutePath, config.tlsConfig.privateKey.absolutePath)
                }
                TlsConfig.None -> "unreachable"
            }
        }}
        javalinConfig.registerPlugin(sslPlugin)
    }
        .ws("/minecraft-chat") { ws ->
            ws.onConnect { ctx ->
                logger.info { "WebSocket connected: ${ctx.host()}" }
                ctx.enableAutomaticPings()
                val handler = broadcastingMessage.register { (message) ->
                    val response = message.toChatResponse(minecraftServer, config.language)
                    ctx.send(Json.encodeToString(response))
                }
                ctx.attribute("handler", handler)
            }
            ws.onClose { ctx ->
                logger.info { "WebSocket closed: ${ctx.host()}" }
                val handler = ctx.attribute<RegisteredEventHandler<*, *>>("handler")
                handler?.unregister()
            }
            ws.onMessage { ctx ->
                val message = ctx.message()
                val req = Json.decodeFromString<ChatRequest>(message)
                val component: Component = when (req) {
                    is ChatRequest.Literal -> Component.literal(req.text)
                    is ChatRequest.Json -> {
                        try {
                            Component.Serializer.fromJson(req.json, minecraftServer.registryAccess())!!
                        } catch (_: Exception) {
                            // Send the raw json text
                            Component.literal(req.json)
                        }
                    }
                }
                minecraftServer.playerList.noBroadCastingMessageEventCurrentThread {
                    broadcastSystemMessage(component, false)
                }
            }
        }
        .start(config.port)

    fun stop() {
        app.stop()
        broadcastingMessage.removeProxy()
    }
}

private fun Component.toChatResponse(minecraftServer: MinecraftServer, language: Language): ChatResponse {
    val json = Component.Serializer.toJson(this, minecraftServer.registryAccess())
    val translatedText = ServerTranslate.translate(this, language)
    return ChatResponse(json, translatedText)
}
