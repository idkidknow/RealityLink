package com.idkidknow.realitylink.forge1165.mixin.mixin;

import com.idkidknow.realitylink.forge1165.mixin.BroadcastingMessage;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;


@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "broadcastMessage", at = @At("RETURN"))
    private void getSystemMessageToBroadcast(Component arg, ChatType arg2, UUID uUID, CallbackInfo ci) {
        if (BroadcastingMessage.isIgnoreMessage()) return;
        BroadcastingMessage.getCallback().accept(arg);
    }
}
