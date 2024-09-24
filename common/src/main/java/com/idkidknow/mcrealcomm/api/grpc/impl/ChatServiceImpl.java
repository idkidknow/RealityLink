package com.idkidknow.mcrealcomm.api.grpc.impl;

import com.idkidknow.mcrealcomm.grpc.ChatComponent;
import com.idkidknow.mcrealcomm.grpc.ChatServiceGrpc;
import com.idkidknow.mcrealcomm.grpc.Empty;
import com.idkidknow.mcrealcomm.grpc.Message;
import com.mojang.logging.LogUtils;
import io.grpc.stub.StreamObserver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private static final Logger logger = LogUtils.getLogger();

    private final MinecraftServer minecraftServer;
    private final Set<StreamObserver<ChatComponent>> chatObservers = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );

    public ChatServiceImpl(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    @Override
    public StreamObserver<Empty> subscribeChat(StreamObserver<ChatComponent> responseObserver) {
        return new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                chatObservers.add(responseObserver);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("subscribeChat error: ", t);
                chatObservers.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                chatObservers.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void broadcastMessage(Message request, StreamObserver<Empty> responseObserver) {
        Component component;
        if (request.hasComponentJson()) {
            try {
                component = Component.Serializer.fromJson(request.getComponentJson(), minecraftServer.registryAccess());
            } catch (Throwable e) {
                logger.error("broadcastMessage error: ", e);
                return;
            }
            if (component == null) return;
        } else if (request.hasLiteral()) {
            component = Component.literal(request.getLiteral());
        } else {
            throw new RuntimeException("Unreachable");
        }
        minecraftServer.getPlayerList().broadcastSystemMessage(component, false);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    public void sendChat(ChatComponent message) {
        for (var observer : chatObservers) {
            observer.onNext(message);
        }
    }

    public void shutdown() {
        for (var observer : chatObservers) {
            observer.onCompleted();
        }
        chatObservers.clear();
    }
}
