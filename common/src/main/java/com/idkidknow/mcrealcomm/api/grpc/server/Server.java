package com.idkidknow.mcrealcomm.api.grpc.server;

import com.idkidknow.mcrealcomm.api.grpc.impl.ChatServiceImpl;
import com.idkidknow.mcrealcomm.grpc.*;
import com.idkidknow.mcrealcomm.server.l10n.ServerTranslate;
import com.mojang.logging.LogUtils;
import io.grpc.ServerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;

public class Server {
    private Server() {}

    private static final Logger logger = LogUtils.getLogger();

    private MinecraftServer minecraftServer;
    private io.grpc.Server grpcServer;
    private ChatServiceImpl chatService;
    private ServerLaunchOption option;

    public static @NotNull Server start(MinecraftServer minecraftServer, ServerLaunchOption option) throws IOException {
        logger.info("Reality Communication grpc server starting");
        Server server = new Server();
        server.minecraftServer = minecraftServer;
        server.chatService = new ChatServiceImpl(minecraftServer);
        server.option = option;
        server.grpcServer = ServerBuilder.forPort(option.port()).addService(server.chatService).build().start();
        BroadcastMessageEvent.register(server::onMessageBroadcast);
        return server;
    }

    public void stop() {
        BroadcastMessageEvent.unregister(this::onMessageBroadcast);
        chatService.shutdown();
        grpcServer.shutdown();
    }

    private void onMessageBroadcast(Component message) {
        var messageTransformed = transformComponent(message);
        chatService.sendChat(messageTransformed);
    }

    private @NotNull ChatComponent transformComponent(@NotNull Component component) {
        var json = Component.Serializer.toJson(component, minecraftServer.registryAccess());
        var builder = ChatComponent.newBuilder()
                .setJson(json);
        if (option.language().isPresent()) {
            builder.setTranslatedText(ServerTranslate.translate(component, option.language().get()));
        }
        return builder.build();
    }
}
