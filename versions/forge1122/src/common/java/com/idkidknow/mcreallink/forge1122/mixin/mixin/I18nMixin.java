package com.idkidknow.mcreallink.forge1122.mixin.mixin;

import com.idkidknow.mcreallink.forge1122.mixin.complement.ServerTranslate;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.translation.LanguageMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("deprecation")
@Mixin(I18n.class)
public abstract class I18nMixin {
    @Inject(method = "translateToLocal", at = @At("HEAD"), cancellable = true)
    private static void translateToLocalWithMyMap(String key, CallbackInfoReturnable<String> cir) {
        LanguageMap language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            cir.setReturnValue(language.translateKey(key));
        }
    }

    @Inject(method = "translateToLocalFormatted", at = @At("HEAD"), cancellable = true)
    private static void translateToLocalFormattedWithMyMap(String string, Object[] format, CallbackInfoReturnable<String> cir) {
        LanguageMap language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            cir.setReturnValue(language.translateKeyFormat(string, format));
        }
    }

    @Inject(method = "canTranslate", at = @At("HEAD"), cancellable = true)
    private static void canTranslateWithMyMap(String key, CallbackInfoReturnable<Boolean> cir) {
        LanguageMap language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            cir.setReturnValue(language.isKeyTranslated(key));
        }
    }

    @Inject(method = "getLastTranslationUpdateTimeInMilliseconds", at = @At("HEAD"), cancellable = true)
    private static void getLastTranslationUpdateTimeInMillisecondsWithMyMap(CallbackInfoReturnable<Long> cir) {
        LanguageMap language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            cir.setReturnValue(language.getLastUpdateTimeInMilliseconds());
        }
    }
}
