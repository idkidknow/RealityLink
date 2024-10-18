package com.idkidknow.mcreallink.platform

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.PlayerList
import java.nio.file.Path

interface Platform : PlatformApi {
    val events: PlatformEvents
}

interface PlatformApi {
    val gameRootDir: Path
    val gameConfigDir: Path
    fun translate(text: FormattedText, language: Language): String
    fun broadcastMessageWithoutCallback(playerList: PlayerList, message: Component)
}

data class ServerStartingEvent(val server: MinecraftServer)
data class ServerStoppingEvent(val server: MinecraftServer)
data class RegisterCommandsEvent(val dispatcher: CommandDispatcher<CommandSourceStack>)
data class BroadcastingMessageEvent(val message: Component)

interface PlatformEvents {
    fun serverStartingCallback(callback: (ServerStartingEvent) -> Unit)
    fun serverStoppingCallback(callback: (ServerStoppingEvent) -> Unit)
    fun registerCommandsCallback(callback: (RegisterCommandsEvent) -> Unit)
    fun broadcastingMessageCallback(callback: (BroadcastingMessageEvent) -> Unit)
}

operator fun PlatformEvents.invoke(action: PlatformEvents.() -> Unit) {
    action(this)
}
