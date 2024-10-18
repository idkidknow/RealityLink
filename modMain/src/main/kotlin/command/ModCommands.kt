package com.idkidknow.mcreallink.command

import com.idkidknow.mcreallink.MOD_ID
import com.idkidknow.mcreallink.ModMain
import com.idkidknow.mcreallink.l10n.LanguageLoadingException
import io.github.oshai.kotlinlogging.KotlinLogging
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

private val logger = KotlinLogging.logger {}

fun modCommandBuilder(
    startAction: () -> Exception?,
    stopAction: () -> Unit,
) = Commands.literal(MOD_ID)
    .requires { it.hasPermission(2) }
    .then(startCommandBuilder(startAction))
    .then(stopCommandBuilder(stopAction))

private fun startCommandBuilder(startAction: () -> Exception?) =
    Commands.literal("start").executes { context ->
        val ret = startAction()
        when (ret) {
            null -> {
                context.source.sendSuccess({ Component.literal("Success.") }, false)
                return@executes 1
            }

            is ModMain.StartApiServerException.AlreadyStarted -> {
                context.source.sendFailure(Component.literal("Already started."))
                return@executes -1
            }

            is ModMain.StartApiServerException.LanguageLoading -> {
                val (e) = ret
                logger.error(e) { "$e" }
                when (e) {
                    is LanguageLoadingException.IOException -> {
                        context.source.sendFailure(Component.literal("Read language file failed: ${e.message}"))
                        return@executes -2
                    }
                    is LanguageLoadingException.ParseException -> {
                        context.source.sendFailure(Component.literal("Parse language file failed: ${e.message}."))
                        return@executes -3
                    }
                }
            }

            else -> {
                throw ret
            }
        }
    }

private fun stopCommandBuilder(stopAction: () -> Unit) =
    Commands.literal("stop").executes { context ->
        stopAction()
        context.source.sendSuccess({ Component.literal("Success.") }, false)
        return@executes 1
    }
