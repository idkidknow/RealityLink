package com.idkidknow.realitylink.platform

import net.minecraft.command.CommandBase
import net.minecraft.command.CommandException
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos

object ModCommand extends CommandBase {
  // scalafix:off DisableSyntax.var
  var startAction: () => Either[Throwable, Unit] = () => Right(())
  var stopAction: () => Unit = () => {}
  var downloadAction: () => Unit = () => {}
  // scalafix:on
  override def getName: String = "realitylink"
  override def getUsage(player: ICommandSender): String =
    "/realitylink start\n/realitylink stop\n/realitylink download"
  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  override def execute(
      server: MinecraftServer,
      sender: ICommandSender,
      args: Array[String],
  ): Unit = {
    args match {
      case Array("start") =>
        startAction() match {
          case Left(e) => throw CommandException("Failed to start", e)
          case _ =>
        }
      case Array("stop") =>
        stopAction()
      case Array("download") =>
        downloadAction()
      case _ => throw CommandException("Invalid arguments")
    }
  }
  override def getTabCompletions(
      server: MinecraftServer,
      sender: ICommandSender,
      args: Array[String],
      targetPos: BlockPos,
  ): java.util.List[String] = {
    if (args.length == 1) // scalafix:ok
      CommandBase.getListOfStringsMatchingLastWord(
        args,
        "start",
        "stop",
        "download",
      )
    else java.util.Collections.emptyList
  }
}
