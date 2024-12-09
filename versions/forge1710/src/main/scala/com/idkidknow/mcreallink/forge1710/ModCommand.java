package com.idkidknow.mcreallink.forge1710;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ModCommand extends CommandBase {
    private ModCommand() {}
    private static final ModCommand instance = new ModCommand();
    public static ModCommand getInstance() { return instance; }

    private Consumer<ICommandSender> startAction = (sender) -> {};
    public void setStartAction(Consumer<ICommandSender> action) {
        this.startAction = action;
    }
    private Consumer<ICommandSender> stopAction = (sender) -> {};
    public void setStopAction(Consumer<ICommandSender> action) {
        this.stopAction = action;
    }

    @Override
    public String getCommandName() {
        return "reallink";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/reallink start\n/reallink stop";
    }

    @Override
    public void processCommand(ICommandSender iCommandSender, String[] args) {
        if (args.length == 0) return;
        if (args[0].equals("start")) {
            startAction.accept(iCommandSender);
        } else if (args[0].equals("stop")) {
            stopAction.accept(iCommandSender);
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender iCommandSender, String[] args) {
        return args.length >= 1 ? getListOfStringsMatchingLastWord(args, "start", "stop") : null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return 0;
    }
}
