package com.idkidknow.mcreallink.forge1710.mixin.mixin;

import com.idkidknow.mcreallink.forge1710.mixin.complement.BroadcastingMessage;
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
        if (!BroadcastingMessage.ignoreMessage()) {
            BroadcastingMessage.callback().apply(component);
        }
    }
}
