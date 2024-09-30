package com.idkidknow.mcrealcomm.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public class RealityCommunicationModEntryFabric implements ModInitializer {
    private static final Logger logger = LogUtils.getLogger();
    @Override
    public void onInitialize() {
        MainKt.fabricModInit();
    }
}
