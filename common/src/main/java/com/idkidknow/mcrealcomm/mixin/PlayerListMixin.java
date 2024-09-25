package com.idkidknow.mcrealcomm.mixin;

import com.idkidknow.mcrealcomm.api.grpc.server.BroadcastMessageEvent;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("RETURN"))
    private void getSystemMessageToBroadcast(Component message, boolean bypassHiddenChat, CallbackInfo ci) {
        BroadcastMessageEvent.invoke(message);
    }
    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At("RETURN"))
    private void getChatMessageToBroadcast(PlayerChatMessage message, Predicate<ServerPlayer> shouldFilterMessageTo, @Nullable ServerPlayer sender, ChatType.Bound boundChatType, CallbackInfo ci) {
        var component = boundChatType.decorate(message.decoratedContent());
        BroadcastMessageEvent.invoke(component);
    }
}
