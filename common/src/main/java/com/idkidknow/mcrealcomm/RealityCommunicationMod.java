package com.idkidknow.mcrealcomm;

import com.google.gson.Gson;
import com.idkidknow.mcrealcomm.api.grpc.server.Server;
import com.idkidknow.mcrealcomm.api.grpc.server.ServerLaunchOption;
import com.idkidknow.mcrealcomm.server.l10n.ServerLanguage;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class RealityCommunicationMod {
    public static final String MOD_ID = "realcomm";

    private static final Logger logger = LogUtils.getLogger();
    private static MinecraftServer server;

    private static Server grpcServer;

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(RealityCommunicationMod::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(RealityCommunicationMod::onServerStopping);
    }

    private static void startApiServer(Path serverOptionFile) {
        record ServerOption(int port, String localeCode, String resourcePacksDir) {}
        ServerOption option;
        try {
            String json = Files.readString(serverOptionFile);
            var gson = new Gson();
            option = gson.fromJson(json, ServerOption.class);
        } catch (IOException e) {
            logger.error("Failed to read {}: ", serverOptionFile, e);
            return;
        }

        logger.info("Reality Communication API server starting");
        var resourcePacksDirStr = option.resourcePacksDir();
        var resourcePacksDir = Platform.getGameFolder().resolve(resourcePacksDirStr);
        var languageResourcePack = ServerLanguage.getResourcePackDirLanguage(resourcePacksDir, option.localeCode());
        var languageFallback = ServerLanguage.getResourceLanguage(server, option.localeCode());
        var language = ServerLanguage.compose(languageResourcePack, languageFallback);
        var launchOption = new ServerLaunchOption(option.port(), Optional.of(language));
        try {
            grpcServer = Server.start(server, launchOption);
        } catch (IOException e) {
            logger.error("Failed to start grpc server: ", e);
        }
    }

    private static void onServerStarting(MinecraftServer server) {
        RealityCommunicationMod.server = server;
        var autostartOptionPath = Platform.getConfigFolder().resolve("realcomm/autostart.json");
        if (!Files.exists(autostartOptionPath)) return;
        logger.info("autostart.json found");
        startApiServer(autostartOptionPath);
    }

    private static void onServerStopping(MinecraftServer server) {
        if (grpcServer == null) return;
        grpcServer.stop();
    }
}
