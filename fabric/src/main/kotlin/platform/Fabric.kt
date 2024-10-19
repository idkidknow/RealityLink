package com.idkidknow.mcreallink.fabric.platform

import com.idkidknow.mcreallink.api.UnitCallbackSet
import com.idkidknow.mcreallink.api.invoke
import com.idkidknow.mcreallink.mixin.complement.BroadcastingMessage
import com.idkidknow.mcreallink.mixin.complement.ServerTranslate
import com.idkidknow.mcreallink.mixin.mixin.MinecraftServerAccessor
import com.idkidknow.mcreallink.mixin.mixin.SimpleReloadableResourceManagerAccessor
import com.idkidknow.mcreallink.platform.BroadcastingMessageEvent
import com.idkidknow.mcreallink.platform.Platform
import com.idkidknow.mcreallink.platform.RegisterCommandsEvent
import com.idkidknow.mcreallink.platform.PlatformEvents
import com.idkidknow.mcreallink.platform.ServerStartingEvent
import com.idkidknow.mcreallink.platform.ServerStoppingEvent
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.locale.Language
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.PlayerList
import java.nio.file.Path

object Fabric: Platform {
    override val events: PlatformEvents = FabricEvents
    override val gameRootDir: Path get() = FabricLoader.getInstance().gameDir
    override val gameConfigDir: Path get() = FabricLoader.getInstance().configDir

    override fun translate(text: FormattedText, language: Language): String = ServerTranslate.translate(text, language)

    override fun broadcastMessageWithoutCallback(playerList: PlayerList, message: Component) {
        BroadcastingMessage.ignoreTemporarily {
            playerList.broadcastMessage(message, ChatType.SYSTEM, net.minecraft.Util.NIL_UUID)
        }
    }

    override fun getNamespaces(server: MinecraftServer): Iterable<String> =
        ((server as MinecraftServerAccessor).resources.resourceManager as SimpleReloadableResourceManagerAccessor).namespaces
}

object FabricEvents : PlatformEvents {
    override fun serverStartingCallback(callback: (ServerStartingEvent) -> Unit) {
        ServerLifecycleEvents.SERVER_STARTING.register { server -> callback(ServerStartingEvent(server)) }
    }

    override fun serverStoppingCallback(callback: (ServerStoppingEvent) -> Unit) {
        ServerLifecycleEvents.SERVER_STOPPING.register { server -> callback(ServerStoppingEvent(server)) }
    }

    override fun registerCommandsCallback(callback: (RegisterCommandsEvent) -> Unit) {
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
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
