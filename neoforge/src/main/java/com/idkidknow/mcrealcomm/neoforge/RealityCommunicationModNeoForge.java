package com.idkidknow.mcrealcomm.neoforge;

import com.idkidknow.mcrealcomm.RealityCommunicationMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Mod(RealityCommunicationMod.MOD_ID)
public final class RealityCommunicationModNeoForge {
    public RealityCommunicationModNeoForge() {
        RealityCommunicationMod.init();
        NeoForge.EVENT_BUS.addListener(RealityCommunicationModNeoForge::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var start = Commands.literal("start")
                .then(Commands.argument("option", StringArgumentType.string()).suggests(new PathSuggestionProvider())
                        .executes(context -> {
                            var option = StringArgumentType.getString(context, "option");
                            var path = RealityCommunicationMod.CONFIG_PATH.resolve("realcomm").resolve(Path.of(option));
                            try {
                                RealityCommunicationMod.startApiServer(path);
                            } catch (IllegalStateException e) {
                                context.getSource().sendFailure(Component.literal("server already started"));
                                return -1;
                            } catch (IllegalArgumentException e) {
                                context.getSource().sendFailure(Component.literal("illegal path"));
                                return -2;
                            } catch (IOException e) {
                                context.getSource().sendFailure(Component.literal("failed"));
                                return -3;
                            }
                            context.getSource().sendSuccess(() -> Component.literal("success"), false);
                            return 1;
                        })
                );
        var stop = Commands.literal("stop").executes(context -> {
            RealityCommunicationMod.stopApiServer();
            context.getSource().sendSuccess(() -> Component.literal("success"), false);
            return 1;
        });
        var builder = Commands.literal("realcomm")
                .requires(source -> source.hasPermission(2))
                .then(start).then(stop);
        dispatcher.register(builder);
    }
}

class PathSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        var path = RealityCommunicationMod.CONFIG_PATH.resolve("realcomm");
        try (var files = Files.list(path)) {
            files.forEach(file -> builder.suggest(file.getFileName().toString()));
        } catch (IOException ignored) {}
        return builder.buildFuture();
    }
}

