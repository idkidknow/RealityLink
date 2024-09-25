package com.idkidknow.mcrealcomm.api.grpc.server;

import io.grpc.ServerCredentials;
import net.minecraft.locale.Language;

import java.util.Optional;

/**
 * @param creds use raw grpc if empty
 * @param language disable server translating if empty
 * */
public record ServerLaunchOption(
        int port,
        ServerCredentials creds,
        Optional<Language> language
) {
}
