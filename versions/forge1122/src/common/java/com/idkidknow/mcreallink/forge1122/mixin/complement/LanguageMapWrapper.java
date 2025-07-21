package com.idkidknow.mcreallink.forge1122.mixin.complement;

import net.minecraft.util.text.translation.LanguageMap;

/// Fix compatibility issues with IndustrialCraft 2
///
/// During initialization, IndustrialCraft 2 uses reflection in [[ic2.core.init.Localization#getLanguageMapMap]]
/// to traverse all methods of [[LanguageMap]]. It filters for the `getInstance` method based on return values,
/// which may incorrectly identify the
/// [[com.idkidknow.mcreallink.forge1122.mixin.mixin.LanguageMapMixin#reallink$getDefault]]
/// method from this mod's [[LanguageMapMutator]].
/// A single-layer wrapper is used to resolve this issue.
public class LanguageMapWrapper {
    private final LanguageMap inner;
    public LanguageMapWrapper(LanguageMap inner) {
        this.inner = inner;
    }

    public LanguageMap get() {
        return inner;
    }
}
