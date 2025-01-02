package com.idkidknow.mcreallink.forge1182.core

import net.minecraft.commands.Commands
import net.minecraft.network.chat.TextComponent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent

def modCommandBuilder(
    startAction: () => Either[Throwable, Unit],
    stopAction: () => Unit,
    downloadAction: () => Unit,
) = Commands.literal("reallink")
  .requires(_.hasPermission(2))
  .`then`(startCommandBuilder(startAction))
  .`then`(stopCommandBuilder(stopAction))
  .`then`(downloadCommandBuilder(downloadAction))

def startCommandBuilder(startAction: () => Either[Throwable, Unit]) = Commands.literal("start")
  .executes { context =>
    startAction() match {
      case Left(e: Exception) => {
        context.getSource.sendFailure(TextComponent(s"Failed: ${e.getMessage}\n Check the log for details."))
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

def downloadCommandBuilder(downloadAction: () => Unit) = Commands.literal("download")
  .executes { context =>
    downloadAction()
    context.getSource.sendSuccess(TextComponent("Downloading..."), false)
    1
  }

@SuppressWarnings(Array("org.wartremover.warts.All"))
object InitCommands {
  var startAction: () => Either[Throwable, Unit] = { () => throw RuntimeException("not initialized") }
  var stopAction: () => Unit = { () => throw RuntimeException("not initialized") }
  var downloadAction: () => Unit = { () => throw RuntimeException("not initialized") }

  def init(): Unit = {
    val builder = modCommandBuilder(startAction, stopAction, downloadAction)
    MinecraftForge.EVENT_BUS.addListener[RegisterCommandsEvent] { event =>
      event.getDispatcher.register(builder)
    }
  }
}
