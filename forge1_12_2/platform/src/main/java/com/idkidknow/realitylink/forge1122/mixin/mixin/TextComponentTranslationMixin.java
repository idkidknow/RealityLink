package com.idkidknow.realitylink.forge1122.mixin.mixin;

import com.google.common.collect.ImmutableList;
import com.idkidknow.realitylink.forge1122.mixin.ServerTranslate;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentTranslationFormatException;
import net.minecraft.util.text.translation.I18n;
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

@SuppressWarnings("deprecation")
@Mixin(TextComponentTranslation.class)
public abstract class TextComponentTranslationMixin extends TextComponentBase {
    @Unique
    private final ConcurrentHashMap<Function<String, Optional<String>>, List<ITextComponent>> realitylink$childrenByLanguage =
            new ConcurrentHashMap<>();
    @Unique
    private final ThreadLocal<List<ITextComponent>> realitylink$initializingChildren = new ThreadLocal<>();
    @Shadow
    List<ITextComponent> children;
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
            method = {"iterator", "getUnformattedComponentText"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/util/text/TextComponentTranslation;children:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD
            ),
            require = 2
    )
    private List<ITextComponent> realitylink$getChildren(TextComponentTranslation component) {
        Function<String, Optional<String>> language = ServerTranslate.getInjectingLanguage();
        return language != null ? realitylink$ensureInitialized(language) : this.children;
    }

    @Redirect(
            method = "initializeFromFormat",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/util/text/TextComponentTranslation;children:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private List<ITextComponent> realitylink$getChildrenForInitialization(TextComponentTranslation component) {
        List<ITextComponent> children = realitylink$initializingChildren.get();
        return children != null ? children : this.children;
    }

    @Unique
    private List<ITextComponent> realitylink$ensureInitialized(Function<String, Optional<String>> language) {
        List<ITextComponent> cachedChildren = realitylink$childrenByLanguage.get(language);
        if (cachedChildren != null) {
            return cachedChildren;
        }

        List<ITextComponent> initializedChildren = realitylink$initializeChildren(language);
        List<ITextComponent> existing = realitylink$childrenByLanguage.putIfAbsent(language, initializedChildren);
        return existing != null ? existing : initializedChildren;
    }

    @Unique
    private List<ITextComponent> realitylink$initializeChildren(Function<String, Optional<String>> language) {
        List<ITextComponent> children = new ArrayList<>();
        List<ITextComponent> previous = realitylink$initializingChildren.get();
        realitylink$initializingChildren.set(children);
        try {
            this.initializeFromFormat(
                    language.apply(this.key).orElseGet(() -> I18n.translateToFallback(this.key))
            );
        } catch (TextComponentTranslationFormatException e) {
            children.clear();
            try {
                this.initializeFromFormat(I18n.translateToFallback(this.key));
            } catch (TextComponentTranslationFormatException fallbackError) {
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
