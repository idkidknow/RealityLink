package com.idkidknow.mcreallink.forge118.minecraft

import cats.syntax.all.*
import net.minecraft.commands.Commands
import net.minecraft.network.chat.TextComponent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent

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

@SuppressWarnings(Array("org.wartremover.warts.All"))
object InitCommands {
  var startAction: () => Either[Throwable, Unit] = { () => throw RuntimeException("not initialized") }
  var stopAction: () => Unit = { () => throw RuntimeException("not initialized") }

  def init(): Unit = {
    val builder = modCommandBuilder(startAction, stopAction)
    MinecraftForge.EVENT_BUS.addListener[RegisterCommandsEvent] { event =>
      event.getDispatcher.register(builder)
    }
  }
}
