package com.idkidknow.mcreallink.fabric;

import net.fabricmc.api.ModInitializer;

public class FabricModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        MainKt.fabricModInit();
    }
}
