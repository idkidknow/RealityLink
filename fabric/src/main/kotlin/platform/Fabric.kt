package com.idkidknow.mcrealcomm.fabric.platform

import com.idkidknow.mcrealcomm.api.CallbackSet
import com.idkidknow.mcrealcomm.api.UnitCallbackSet
import com.idkidknow.mcrealcomm.api.invoke
import com.idkidknow.mcrealcomm.mixin.complement.BroadcastingMessage
import com.idkidknow.mcrealcomm.mixin.complement.ServerTranslate
import com.idkidknow.mcrealcomm.platform.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.platform.Platform
import com.idkidknow.mcrealcomm.platform.RegisterCommandsEvent
import com.idkidknow.mcrealcomm.platform.PlatformEvents
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.players.PlayerList
import java.nio.file.Path

object Fabric: Platform {
    override val events: PlatformEvents = FabricEvents
    override val gameRootDir: Path get() = FabricLoader.getInstance().gameDir
    override val gameConfigDir: Path get() = FabricLoader.getInstance().configDir

    override fun translate(text: FormattedText, language: Language): String = ServerTranslate.translate(text, language)

    override fun broadcastMessageWithoutCallback(playerList: PlayerList, message: Component) {
        BroadcastingMessage.ignoreTemporarily {
            playerList.broadcastSystemMessage(message, false)
        }
    }
}

object FabricEvents : PlatformEvents {
    override fun serverStartingCallback(callback: (ServerStartingEvent) -> Unit) {
        ServerLifecycleEvents.SERVER_STARTING.register { server -> callback(ServerStartingEvent(server)) }
    }

    override fun serverStoppingCallback(callback: (ServerStoppingEvent) -> Unit) {
        ServerLifecycleEvents.SERVER_STOPPING.register { server -> callback(ServerStoppingEvent(server)) }
    }

    override fun registerCommandsCallback(callback: (RegisterCommandsEvent) -> Unit) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            callback(RegisterCommandsEvent(dispatcher))
        }
    }

    val broadcastingMessageCallbackSet = UnitCallbackSet<BroadcastingMessageEvent>()
    init {
        BroadcastingMessage.callback = { message ->
            broadcastingMessageCallbackSet.invoke(BroadcastingMessageEvent(message))
        }
    }
    override fun broadcastingMessageCallback(callback: (BroadcastingMessageEvent) -> Unit) {
        broadcastingMessageCallbackSet.addCallback(callback)
    }
}
