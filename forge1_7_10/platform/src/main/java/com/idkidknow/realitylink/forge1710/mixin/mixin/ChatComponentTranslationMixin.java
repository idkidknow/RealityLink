package com.idkidknow.realitylink.forge1710.mixin.mixin;

import com.google.common.collect.ImmutableList;
import com.idkidknow.realitylink.forge1710.mixin.ServerTranslate;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatComponentTranslationFormatException;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mixin(ChatComponentTranslation.class)
public abstract class ChatComponentTranslationMixin extends ChatComponentStyle {
    @Unique
    private final ConcurrentHashMap<Function<String, Optional<String>>, List<IChatComponent>> realitylink$childrenByLanguage =
            new ConcurrentHashMap<>();
    @Unique
    private final ThreadLocal<List<IChatComponent>> realitylink$initializingChildren = new ThreadLocal<>();
    @Shadow
    List<IChatComponent> children;
    @Shadow
    @Final
    private String key;

    @Shadow
    protected abstract void initializeFromFormat(String format);

    @Inject(method = "ensureInitialized", at = @At("HEAD"), cancellable = true)
    private void realitylink$ensureInitializedWithInjectedLanguage(CallbackInfo ci) {
        Function<String, Optional<String>> language = ServerTranslate.getInjectingLanguage();
        if (language != null) {
            realitylink$ensureInitialized(language);
            ci.cancel();
        }
    }

    @Redirect(
            method = {"iterator", "getUnformattedTextForChat"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/util/ChatComponentTranslation;children:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD
            ),
            require = 2
    )
    private List<IChatComponent> realitylink$getChildren(ChatComponentTranslation component) {
        Function<String, Optional<String>> language = ServerTranslate.getInjectingLanguage();
        return language != null ? realitylink$ensureInitialized(language) : this.children;
    }

    @Redirect(
            method = "initializeFromFormat",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/util/ChatComponentTranslation;children:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private List<IChatComponent> realitylink$getChildrenForInitialization(ChatComponentTranslation component) {
        List<IChatComponent> children = realitylink$initializingChildren.get();
        return children != null ? children : this.children;
    }

    @Unique
    private List<IChatComponent> realitylink$ensureInitialized(Function<String, Optional<String>> language) {
        List<IChatComponent> cachedChildren = realitylink$childrenByLanguage.get(language);
        if (cachedChildren != null) {
            return cachedChildren;
        }

        List<IChatComponent> initializedChildren = realitylink$initializeChildren(language);
        List<IChatComponent> existing = realitylink$childrenByLanguage.putIfAbsent(language, initializedChildren);
        return existing != null ? existing : initializedChildren;
    }

    @Unique
    private List<IChatComponent> realitylink$initializeChildren(Function<String, Optional<String>> language) {
        List<IChatComponent> children = new ArrayList<>();
        List<IChatComponent> previous = realitylink$initializingChildren.get();
        realitylink$initializingChildren.set(children);
        try {
            this.initializeFromFormat(
                    language.apply(this.key).orElseGet(() -> StatCollector.translateToFallback(this.key))
            );
        } catch (ChatComponentTranslationFormatException e) {
            children.clear();
            try {
                this.initializeFromFormat(StatCollector.translateToFallback(this.key));
            } catch (ChatComponentTranslationFormatException fallbackError) {
                throw e;
            }
        } finally {
            if (previous == null) {
                realitylink$initializingChildren.remove();
            } else {
                realitylink$initializingChildren.set(previous);
            }
        }
        return ImmutableList.copyOf(children);
    }
}
