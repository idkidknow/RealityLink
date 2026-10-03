package com.idkidknow.realitylink.forge1165.mixin.mixin;

import com.google.common.collect.ImmutableList;
import com.idkidknow.realitylink.forge1165.mixin.ServerTranslate;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableFormatException;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.TextComponentMessageFormatHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TranslatableComponent has a mutable cache that can cause race conditions with RealityLink.
 * This mixin resolves them by adding an independent cache.
 */
@Mixin(TranslatableComponent.class)
public abstract class TranslatableComponentMixin {
    @Unique
    private final ConcurrentHashMap<Language, List<FormattedText>> realitylink$decomposedParts =
            new ConcurrentHashMap<>();
    @Shadow
    @Final
    private String key;

    @Shadow
    @Final
    private Object[] args;
    @Shadow
    @Final
    private static Pattern FORMAT_PATTERN;
    @Shadow
    @Final
    private static FormattedText TEXT_PERCENT;

    @Shadow
    private FormattedText getArgument(int index) {
        throw new AssertionError();
    }

    @Inject(
            method = "visitSelf(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void realitylink$visitStyledWithInjectedLanguage(
            FormattedText.StyledContentConsumer<T> consumer,
            Style style,
            CallbackInfoReturnable<Optional<T>> cir
    ) {
        Language language = ServerTranslate.getInjectingLanguage();
        if (language == null) {
            return;
        }

        for (FormattedText part : realitylink$decompose(language)) {
            Optional<T> result = part.visit(consumer, style);
            if (result.isPresent()) {
                cir.setReturnValue(result);
                return;
            }
        }

        cir.setReturnValue(Optional.empty());
    }

    @Inject(
            method = "visitSelf(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void realitylink$visitWithInjectedLanguage(
            FormattedText.ContentConsumer<T> consumer,
            CallbackInfoReturnable<Optional<T>> cir
    ) {
        Language language = ServerTranslate.getInjectingLanguage();
        if (language == null) {
            return;
        }

        for (FormattedText part : realitylink$decompose(language)) {
            Optional<T> result = part.visit(consumer);
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
        String template = language.getOrDefault(this.key);
        try {
            List<FormattedText> parts = new ArrayList<>();
            realitylink$decomposeTemplate(template, parts);
            return ImmutableList.copyOf(parts);
        } catch (TranslatableFormatException e) {
            return ImmutableList.of(FormattedText.of(template));
        }
    }

    @Unique
    private void realitylink$decomposeTemplate(String formatTemplate, List<FormattedText> parts) {
        // The original parser writes directly to the cache, so I have to ...
        Matcher matcher = FORMAT_PATTERN.matcher(formatTemplate);
        TranslatableComponent component = (TranslatableComponent) (Object) this;
        try {
            int argumentIndex = 0;
            int position = 0;
            while (matcher.find(position)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > position) {
                    String literal = formatTemplate.substring(position, start);
                    if (literal.indexOf('%') != -1) {
                        throw new IllegalArgumentException();
                    }
                    parts.add(FormattedText.of(literal));
                }

                String format = matcher.group(2);
                String token = formatTemplate.substring(start, end);
                if ("%".equals(format) && "%%".equals(token)) {
                    parts.add(TEXT_PERCENT);
                } else {
                    if (!"s".equals(format)) {
                        throw new TranslatableFormatException(component, "Unsupported format: '" + token + "'");
                    }
                    String explicitIndex = matcher.group(1);
                    int index = explicitIndex != null ? Integer.parseInt(explicitIndex) - 1 : argumentIndex++;
                    if (index < this.args.length) {
                        parts.add(this.getArgument(index));
                    }
                }
                position = end;
            }

            if (position == 0) {
                position = TextComponentMessageFormatHandler.handle(component, parts, this.args, formatTemplate);
            }
            if (position < formatTemplate.length()) {
                String literal = formatTemplate.substring(position);
                if (literal.indexOf('%') != -1) {
                    throw new IllegalArgumentException();
                }
                parts.add(FormattedText.of(literal));
            }
        } catch (IllegalArgumentException e) {
            throw new TranslatableFormatException(component, e);
        }
    }
}
