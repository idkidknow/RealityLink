package com.idkidknow.mcreallink.forge;

import net.minecraftforge.fml.common.Mod;

@Mod(com.idkidknow.mcreallink.MainKt.MOD_ID)
public final class ForgeModEntry {
    public ForgeModEntry() {
        MainKt.forgeModInit();
    }
}
