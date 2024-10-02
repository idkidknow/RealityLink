package com.idkidknow.mcrealcomm.platform

import com.idkidknow.mcrealcomm.event.UnitEventManager
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.MinecraftServer

data class ServerStartingEvent(val server: MinecraftServer)
data class ServerStoppingEvent(val server: MinecraftServer)

data class ServerLifecycleEventManagers(
    val serverStarting: UnitEventManager<ServerStartingEvent>,
    val serverStopping: UnitEventManager<ServerStoppingEvent>,
)

data class RegisterCommandsEvent(
    val dispatcher: CommandDispatcher<CommandSourceStack>,
    val environment: Commands.CommandSelection,
    val context: CommandBuildContext,
)
