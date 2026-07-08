package com.idkidknow.realitylink.forge1122.mixin;

import net.minecraft.util.text.translation.LanguageMap;

import java.util.function.Function;

public interface LanguageMapMutator {
    void realitylink$setFunction(Function<String, String> func);
    LanguageMapWrapper realitylink$getDefault();

    static LanguageMap make(Function<String, String> func) {
        LanguageMap ret = new LanguageMap();
        ((LanguageMapMutator) (Object) ret).realitylink$setFunction(func);
        return ret;
    }

    static LanguageMap getDefault() {
        return (((LanguageMapMutator) (Object) new LanguageMap()).realitylink$getDefault()).get();
    }
}
