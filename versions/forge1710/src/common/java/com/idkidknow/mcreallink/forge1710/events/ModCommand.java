package com.idkidknow.mcreallink.forge1710.events;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ModCommand extends CommandBase {
    private ModCommand() {}
    private static final ModCommand instance = new ModCommand();
    public static ModCommand getInstance() { return instance; }

    private Supplier<Optional<Throwable>> startAction = Optional::empty;
    public void setStartAction(Supplier<Optional<Throwable>> action) {
        this.startAction = action;
    }
    private Runnable stopAction = () -> {};
    public void setStopAction(Runnable action) {
        this.stopAction = action;
    }
    private Runnable downloadAction = () -> {};
    public void setDownloadAction(Runnable action) {
        this.downloadAction = action;
    }

    @Override
    public String getCommandName() {
        return "reallink";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/reallink start\n/reallink stop\n/reallink download";
    }

    @Override
    public void processCommand(ICommandSender iCommandSender, String[] args) {
        if (args.length == 0) return;
        if (args[0].equals("start")) {
            Optional<Throwable> ret = startAction.get();
            if (ret.isPresent()) {
                throw new CommandException("Failed", ret.get());
            }
        } else if (args[0].equals("stop")) {
            stopAction.run();
        } else if (args[0].equals("download")) {
            downloadAction.run();
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length >= 1 ? getListOfStringsMatchingLastWord(args, "start", "stop", "download") : null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return this.getCommandName().compareTo(((ICommand) o).getCommandName());
    }
}
