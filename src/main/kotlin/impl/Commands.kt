package com.idkidknow.mcreallink.platform.neoforge121.impl

import com.idkidknow.mcreallink.MOD_ID
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

fun modCommandBuilder(
    startAction: () -> Result<Unit>,
    stopAction: () -> Unit,
) = Commands.literal(MOD_ID)
    .requires { it.hasPermission(2) }
    .then(startCommandBuilder(startAction))
    .then(stopCommandBuilder(stopAction))

fun startCommandBuilder(startAction: () -> Result<Unit>) = Commands.literal("start")
    .executes { context ->
        startAction().onSuccess {
            context.source.sendSuccess({ Component.literal("Success.") }, false)
            return@executes 1
        }.onFailure { e ->
            context.source.sendFailure(Component.literal("Failed: ${e.message}\n Check the log for details."))
            return@executes -1
        }
        return@executes -2
    }

fun stopCommandBuilder(stopAction: () -> Unit) = Commands.literal("stop")
    .executes { context ->
        stopAction()
        context.source.sendSuccess({ Component.literal("Success.") }, false)
        return@executes 1
    }
