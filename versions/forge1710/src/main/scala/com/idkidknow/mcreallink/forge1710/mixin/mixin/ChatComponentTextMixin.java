package com.idkidknow.mcreallink.forge1710.mixin.mixin;

import com.idkidknow.mcreallink.forge1710.mixin.complement.ServerTranslate;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

@Mixin(ChatComponentText.class)
public abstract class ChatComponentTextMixin extends ChatComponentStyle {
    @Shadow @Final private String text;

    @Inject(method = "getUnformattedTextForChat", at = @At("HEAD"), cancellable = true)
    private void fixTextComponentWithSiblings(CallbackInfoReturnable<String> cir) {
        Function<String, Optional<String>> language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(this.text);
            Iterator it = ChatComponentStyle.createDeepCopyIterator(this.siblings);
            for (Iterator iter = it; iter.hasNext(); ) {
                IChatComponent component = (IChatComponent) iter.next();
                builder.append(component.getUnformattedTextForChat());
            }
            cir.setReturnValue(builder.toString());
        }
    }
}
