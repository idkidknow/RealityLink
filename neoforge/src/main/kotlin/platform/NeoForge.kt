package com.idkidknow.mcrealcomm.neoforge.platform

import com.idkidknow.mcrealcomm.api.UnitCallbackSet
import com.idkidknow.mcrealcomm.api.invoke
import com.idkidknow.mcrealcomm.mixin.complement.BroadcastingMessage
import com.idkidknow.mcrealcomm.mixin.complement.ServerTranslate
import com.idkidknow.mcrealcomm.platform.BroadcastingMessageEvent
import com.idkidknow.mcrealcomm.platform.Platform
import com.idkidknow.mcrealcomm.platform.PlatformEvents
import com.idkidknow.mcrealcomm.platform.RegisterCommandsEvent
import com.idkidknow.mcrealcomm.platform.ServerStartingEvent
import com.idkidknow.mcrealcomm.platform.ServerStoppingEvent
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.players.PlayerList
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import java.nio.file.Path

object NeoForge: Platform {
    override val events: PlatformEvents = NeoForgeEvents
    override val gameRootDir: Path get () = FMLPaths.GAMEDIR.get()
    override val gameConfigDir: Path get() = FMLPaths.CONFIGDIR.get()

    override fun translate(text: FormattedText, language: Language): String = ServerTranslate.translate(text, language)

    override fun broadcastMessageWithoutCallback(playerList: PlayerList, message: Component) {
        BroadcastingMessage.ignoreTemporarily {
            playerList.broadcastSystemMessage(message, false)
        }
    }
}

object NeoForgeEvents : PlatformEvents {
    override fun serverStartingCallback(callback: (ServerStartingEvent) -> Unit) {
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStartingEvent> { event ->
            callback(ServerStartingEvent(event.server))
        }
    }

    override fun serverStoppingCallback(callback: (ServerStoppingEvent) -> Unit) {
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStoppingEvent> { event ->
            callback(ServerStoppingEvent(event.server))
        }
    }

    override fun registerCommandsCallback(callback: (RegisterCommandsEvent) -> Unit) {
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.RegisterCommandsEvent> { event ->
            callback(RegisterCommandsEvent(event.dispatcher))
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
