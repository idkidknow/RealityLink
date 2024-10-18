package com.idkidknow.mcreallink.mixin.mixin;

import com.idkidknow.mcreallink.mixin.complement.ServerTranslate;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public abstract class LanguageMixin {
    @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
    private static void replaceLanguageInstance(CallbackInfoReturnable<Language> cir) {
        var language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            cir.setReturnValue(language);
        }
    }
}