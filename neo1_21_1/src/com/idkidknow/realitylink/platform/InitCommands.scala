package com.idkidknow.realitylink.platform

import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

object InitCommands {
  private def modCommandBuilder(
      startAction: () => Either[Throwable, Unit],
      stopAction: () => Unit,
      downloadAction: () => Unit,
  ) = Commands
    .literal("realitylink")
    .requires(_.hasPermission(2))
    .`then`(startCommandBuilder(startAction))
    .`then`(stopCommandBuilder(stopAction))
    .`then`(downloadCommandBuilder(downloadAction))

  private def startCommandBuilder(startAction: () => Either[Throwable, Unit]) =
    Commands
      .literal("start")
      .executes { context =>
        startAction() match {
          case Left(e: Exception) =>
            context.getSource.sendFailure(
              Component
                .literal(
                  s"Failed: ${e.getMessage}\n Check the log for details."
                )
            )
            1
          case Right(_) =>
            context.getSource.sendSuccess(
              () => Component.literal("Success."),
              false,
            )
            -1
          case _ => -2
        }
      }

  private def stopCommandBuilder(stopAction: () => Unit) = Commands
    .literal("stop")
    .executes { context =>
      stopAction()
      context.getSource.sendSuccess(() => Component.literal("Success."), false)
      1
    }

  private def downloadCommandBuilder(downloadAction: () => Unit) = Commands
    .literal("download")
    .executes { context =>
      downloadAction()
      context.getSource.sendSuccess(
        () => Component.literal("Downloading..."),
        false,
      )
      1
    }

  // scalafix:off DisableSyntax.var
  var startAction: () => Either[Throwable, Unit] = () =>
    sys.error("not initialized")
  var stopAction: () => Unit = () => sys.error("not initialized")
  var downloadAction: () => Unit = () => sys.error("not initialized")
  // scalafix:on

  def init(): Unit = {
    val builder = modCommandBuilder(
      () => startAction(),
      () => stopAction(),
      () => downloadAction(),
    )
    NeoForge.EVENT_BUS.addListener[RegisterCommandsEvent] { event =>
      event.getDispatcher.register(builder)
    }
  }
}
