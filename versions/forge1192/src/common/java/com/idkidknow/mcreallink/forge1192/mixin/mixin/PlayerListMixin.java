package com.idkidknow.mcreallink.forge1192.mixin.mixin;

import com.idkidknow.mcreallink.forge1192.mixin.complement.BroadcastingMessage;
import net.minecraft.network.chat.ChatSender;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatSender;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At("RETURN"))
    private void getChatMessageToBroadcast(PlayerChatMessage message, Predicate<ServerPlayer> shouldFilterMessageTo, ServerPlayer senderPlayer, ChatSender chatSender, ChatType.Bound boundChatType, CallbackInfo ci) {
        if (BroadcastingMessage.isIgnoreMessage()) return;
        Component component = boundChatType.decorate(message.serverContent());
        BroadcastingMessage.getCallback().accept(component);
    }
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("RETURN"))
    private void getMessageToBroadcast(Component message, boolean bypassHiddenChat, CallbackInfo ci) {
        if (BroadcastingMessage.isIgnoreMessage()) return;
        BroadcastingMessage.getCallback().accept(message);
    }
}
