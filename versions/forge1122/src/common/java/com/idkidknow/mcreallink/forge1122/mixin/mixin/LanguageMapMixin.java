package com.idkidknow.mcreallink.forge1122.mixin.mixin;

import com.idkidknow.mcreallink.forge1122.mixin.complement.LanguageMapMutator;
import net.minecraft.util.text.translation.LanguageMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(LanguageMap.class)
public abstract class LanguageMapMixin implements LanguageMapMutator {
    @Shadow
    @Final
    private static LanguageMap instance;
    
    @Unique
    private @Nullable Function<String, String> reallink$modified = null;

    @Inject(method = "tryTranslateKey", at = @At("HEAD"), cancellable = true)
    private void overrideTryTranslateKey(String key, CallbackInfoReturnable<String> cir) {
        if (reallink$modified != null) {
            cir.setReturnValue(reallink$modified.apply(key));
        }
    }

    @Override
    public void reallink$setFunction(Function<String, String> func) {
        reallink$modified = func;
    }

    @Override
    public LanguageMap reallink$getDefault() {
        return instance;
    }
}
