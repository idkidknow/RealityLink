package com.idkidknow.mcreallink.forge118.platform

import cats.syntax.all.*
import net.minecraft.commands.Commands
import net.minecraft.network.chat.TextComponent

def modCommandBuilder(
  startAction: () => Either[Throwable, Unit],
  stopAction: () => Unit,
) = Commands.literal("reallink")
  .requires(_.hasPermission(2))
  .`then`(startCommandBuilder(startAction))
  .`then`(stopCommandBuilder(stopAction))

def startCommandBuilder(startAction: () => Either[Throwable, Unit]) = Commands.literal("start")
  .executes { context =>
    startAction() match {
      case Left(e: Exception) => {
        context.getSource.sendFailure(TextComponent(show"Failed: ${e.getMessage}\n Check the log for details."))
        1
      }
      case Right(_) => {
        context.getSource.sendSuccess(TextComponent("Success."), false)
        -1
      }
      case _ => -2
    }
  }

def stopCommandBuilder(stopAction: () => Unit) = Commands.literal("stop")
  .executes { context =>
    stopAction()
    context.getSource.sendSuccess(TextComponent("Success."), false)
    1
  }
