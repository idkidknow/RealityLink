package com.idkidknow.mcreallink.forge.platform

import com.idkidknow.mcreallink.api.UnitCallbackSet
import com.idkidknow.mcreallink.api.invoke
import com.idkidknow.mcreallink.mixin.complement.BroadcastingMessage
import com.idkidknow.mcreallink.mixin.complement.ServerTranslate
import com.idkidknow.mcreallink.platform.BroadcastingMessageEvent
import com.idkidknow.mcreallink.platform.Platform
import com.idkidknow.mcreallink.platform.PlatformEvents
import com.idkidknow.mcreallink.platform.RegisterCommandsEvent
import com.idkidknow.mcreallink.platform.ServerStartingEvent
import com.idkidknow.mcreallink.platform.ServerStoppingEvent
import net.minecraft.locale.Language
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.players.PlayerList
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.loading.FMLPaths
import java.nio.file.Path

object Forge: Platform {
    override val events: PlatformEvents = ForgeEvents
    override val gameRootDir: Path get () = FMLPaths.GAMEDIR.get()
    override val gameConfigDir: Path get() = FMLPaths.CONFIGDIR.get()

    override fun translate(text: FormattedText, language: Language): String = ServerTranslate.translate(text, language)

    override fun broadcastMessageWithoutCallback(playerList: PlayerList, message: Component) {
        BroadcastingMessage.ignoreTemporarily {
            playerList.broadcastMessage(message, ChatType.SYSTEM, net.minecraft.Util.NIL_UUID)
        }
    }
}

object ForgeEvents : PlatformEvents {
    override fun serverStartingCallback(callback: (ServerStartingEvent) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<net.minecraftforge.event.server.ServerStartingEvent> { event ->
            callback(ServerStartingEvent(event.server))
        }
    }

    override fun serverStoppingCallback(callback: (ServerStoppingEvent) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<net.minecraftforge.event.server.ServerStoppingEvent> { event ->
            callback(ServerStoppingEvent(event.server))
        }
    }

    override fun registerCommandsCallback(callback: (RegisterCommandsEvent) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<net.minecraftforge.event.RegisterCommandsEvent> { event ->
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
