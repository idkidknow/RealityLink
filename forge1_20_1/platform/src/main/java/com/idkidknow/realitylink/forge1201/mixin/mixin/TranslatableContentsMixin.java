package com.idkidknow.realitylink.forge1201.mixin.mixin;

import com.google.common.collect.ImmutableList;
import com.idkidknow.realitylink.forge1201.mixin.ServerTranslate;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * TranslatableContents has a mutable cache that can cause race conditions with RealityLink.
 * This mixin resolves them by adding an independent cache.
 */
@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin {
    @Unique
    private final ConcurrentHashMap<Language, List<FormattedText>> realitylink$decomposedParts =
            new ConcurrentHashMap<>();
    @Shadow
    @Final
    private String key;
    @Shadow
    @Final
    @Nullable
    private String fallback;

    @Shadow
    private void decomposeTemplate(String formatTemplate, Consumer<FormattedText> consumer) {
        throw new AssertionError();
    }

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void realitylink$visitStyledWithInjectedLanguage(
            FormattedText.StyledContentConsumer<T> styledContentConsumer,
            Style style,
            CallbackInfoReturnable<Optional<T>> cir
    ) {
        Language language = ServerTranslate.getInjectingLanguage();
        if (language == null) {
            return;
        }

        for (FormattedText part : realitylink$decompose(language)) {
            Optional<T> result = part.visit(styledContentConsumer, style);
            if (result.isPresent()) {
                cir.setReturnValue(result);
                return;
            }
        }

        cir.setReturnValue(Optional.empty());
    }

    @Inject(
            method = "visit(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void realitylink$visitWithInjectedLanguage(
            FormattedText.ContentConsumer<T> contentConsumer,
            CallbackInfoReturnable<Optional<T>> cir
    ) {
        Language language = ServerTranslate.getInjectingLanguage();
        if (language == null) {
            return;
        }

        for (FormattedText part : realitylink$decompose(language)) {
            Optional<T> result = part.visit(contentConsumer);
            if (result.isPresent()) {
                cir.setReturnValue(result);
                return;
            }
        }

        cir.setReturnValue(Optional.empty());
    }

    @Unique
    private List<FormattedText> realitylink$decompose(Language language) {
        List<FormattedText> parts = realitylink$decomposedParts.get(language);
        if (parts != null) {
            return parts;
        }
        parts = realitylink$buildDecomposedParts(language);
        List<FormattedText> existing = realitylink$decomposedParts.putIfAbsent(language, parts);
        return existing != null ? existing : parts;
    }

    @Unique
    private List<FormattedText> realitylink$buildDecomposedParts(Language language) {
        String template = this.fallback != null
                ? language.getOrDefault(this.key, this.fallback)
                : language.getOrDefault(this.key);
        try {
            ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
            this.decomposeTemplate(template, builder::add);
            return builder.build();
        } catch (TranslatableFormatException e) {
            return ImmutableList.of(FormattedText.of(template));
        }
    }
}
