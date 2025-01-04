package com.idkidknow.mcreallink.forge1122.mixin.complement;

import net.minecraft.util.text.translation.LanguageMap;

import java.util.function.Function;

public interface LanguageMapMutator {
    void reallink$setFunction(Function<String, String> func);
    LanguageMap reallink$getDefault();

    static LanguageMap make(Function<String, String> func) {
        LanguageMap ret = new LanguageMap();
        ((LanguageMapMutator) (Object) ret).reallink$setFunction(func);
        return ret;
    }

    static LanguageMap getDefault() {
        return ((LanguageMapMutator) (Object) new LanguageMap()).reallink$getDefault();
    }
}
