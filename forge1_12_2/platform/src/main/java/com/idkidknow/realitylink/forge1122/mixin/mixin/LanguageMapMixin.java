package com.idkidknow.realitylink.forge1122.mixin.mixin;

import com.idkidknow.realitylink.forge1122.mixin.LanguageMapMutator;
import com.idkidknow.realitylink.forge1122.mixin.LanguageMapWrapper;
import net.minecraft.util.text.translation.LanguageMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.function.Function;

@Mixin(LanguageMap.class)
public abstract class LanguageMapMixin implements LanguageMapMutator {
    @Shadow
    @Final
    private static LanguageMap instance;

    @Unique
    private @Nullable Function<String, String> realitylink$modified = null;

    @Inject(method = "tryTranslateKey", at = @At("HEAD"), cancellable = true)
    private void overrideTryTranslateKey(String key, CallbackInfoReturnable<String> cir) {
        if (realitylink$modified != null) {
            cir.setReturnValue(realitylink$modified.apply(key));
        }
    }

    @Override
    public void realitylink$setFunction(Function<String, String> func) {
        realitylink$modified = func;
    }

    @Override
    public LanguageMapWrapper realitylink$getDefault() {
        return new LanguageMapWrapper(instance);
    }
}
