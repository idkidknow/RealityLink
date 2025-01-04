package com.idkidknow.mcreallink.forge1122.mixin.mixin;

import com.idkidknow.mcreallink.forge1122.mixin.complement.BroadcastingMessage;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/util/text/ITextComponent;Z)V", at = @At("RETURN"))
    private void getMessageToBroadcast(ITextComponent component, boolean isSystem, CallbackInfo ci) {
        if (BroadcastingMessage.isIgnoreMessage()) return;
        BroadcastingMessage.getCallback().accept(component);
    }
}