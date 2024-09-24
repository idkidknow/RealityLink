package com.idkidknow.mcrealcomm.api.grpc.server;

import net.minecraft.locale.Language;

import java.util.Optional;

public record ServerLaunchOption(
        int port,
        Optional<Language> language // disable server translating if empty
) {
}
