package com.idkidknow.realitylink.forge1710.mixin.mixin;

import com.idkidknow.realitylink.forge1710.mixin.BroadcastingMessage;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationManager.class)
public class ServerConfigurationManagerMixin {
    @Inject(method = "sendChatMsgImpl", at = @At("HEAD"))
    private void getBroadcastingMessage(IChatComponent component, boolean isChat, CallbackInfo ci) {
        if (!BroadcastingMessage.isIgnoreMessage()) {
            BroadcastingMessage.getCallback().accept(component);
        }
    }
}
