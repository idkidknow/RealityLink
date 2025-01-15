package com.idkidknow.mcreallink.forge1710.mixin.mixin;

import com.idkidknow.mcreallink.forge1710.mixin.complement.ServerTranslate;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatComponentTranslationFormatException;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(ChatComponentTranslation.class)
public abstract class ChatComponentTranslationMixin {
    @Shadow
    protected abstract void initializeFromFormat(String format);

    @Shadow @Final
    private String key;

    @Shadow
    List<IChatComponent> children;

    @Shadow @Final private Object syncLock;

    @Shadow @Final private Object[] formatArgs;

    @Inject(method = "ensureInitialized", at = @At("HEAD"), cancellable = true)
    private void injectEnsureInitialized(CallbackInfo ci) {
        Function<String, Optional<String>> language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            synchronized (this) {
                synchronized (this.syncLock) {
                    this.children.clear();
                }

                try {
                    Optional<String> format = language.apply(this.key);
                    if (format.isPresent()) {
                        this.initializeFromFormat(format.get());
                    } else {
                        this.initializeFromFormat(StatCollector.translateToFallback(this.key));
                    }
                } catch (ChatComponentTranslationFormatException e) {
                    this.children.clear();
                    this.initializeFromFormat(StatCollector.translateToFallback(this.key));
                }

                ci.cancel();
            }
        }
    }
}
