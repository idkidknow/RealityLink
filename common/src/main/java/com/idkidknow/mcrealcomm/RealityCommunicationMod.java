package com.idkidknow.mcrealcomm;

import com.google.gson.Gson;
import com.idkidknow.mcrealcomm.api.grpc.server.Server;
import com.idkidknow.mcrealcomm.api.grpc.server.ServerLaunchOption;
import com.idkidknow.mcrealcomm.server.l10n.ServerLanguage;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class RealityCommunicationMod {
    public static final String MOD_ID = "realcomm";
    public static final Path GAME_PATH = Platform.getGameFolder();
    public static final Path CONFIG_PATH = Platform.getConfigFolder();

    private static final Logger logger = LogUtils.getLogger();
    private static MinecraftServer server;

    private static Server grpcServer;

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(RealityCommunicationMod::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(RealityCommunicationMod::onServerStopping);
    }

    public static void startApiServer(Path serverOptionFile) throws IOException {
        if (grpcServer != null) {
            throw new IllegalStateException("trying to start an API server, but server already started");
        }
        serverOptionFile = CONFIG_PATH.resolve(serverOptionFile);
        if (!serverOptionFile.startsWith(CONFIG_PATH.resolve("realcomm"))) {
            throw new IllegalArgumentException("illegal path");
        }

        record ServerOption(
                int port,
                String localeCode,
                String resourcePacksDir,
                String certChain,
                String privateKey,
                String trustCerts
        ) {}
        ServerOption option;

        try {
            String json = Files.readString(serverOptionFile);
            var gson = new Gson();
            option = gson.fromJson(json, ServerOption.class);
        } catch (IOException e) {
            logger.error("Failed to read {}", serverOptionFile);
            throw e;
        }

        logger.info("Reality Communication API server starting");
        var resourcePacksDirStr = option.resourcePacksDir();
        var resourcePacksDir = GAME_PATH.resolve(resourcePacksDirStr);
        var languageResourcePack = ServerLanguage.getResourcePackDirLanguage(resourcePacksDir, option.localeCode());
        var languageFallback = ServerLanguage.getResourceLanguage(server, option.localeCode());
        var language = ServerLanguage.compose(languageResourcePack, languageFallback);

        ServerCredentials creds;
        if (option.certChain() == null || option.privateKey() == null) {
            if (option.certChain() != null || option.privateKey() != null) {
                throw new IllegalArgumentException("certChain and privateKey both must be provided");
            }
            creds = InsecureServerCredentials.create();
        } else {
            logger.info("set up tls");
            var realcommConfigPath = CONFIG_PATH.resolve("realcomm");
            var certChain = realcommConfigPath.resolve(option.certChain());
            var privateKey = realcommConfigPath.resolve(option.privateKey());
            var tlsBuilder = TlsServerCredentials.newBuilder().keyManager(certChain.toFile(), privateKey.toFile());
            if (option.trustCerts() != null) {
                logger.info("set up mutual tls");
                var trustCerts = realcommConfigPath.resolve(option.trustCerts());
                tlsBuilder.trustManager(trustCerts.toFile());
                tlsBuilder.clientAuth(TlsServerCredentials.ClientAuth.REQUIRE);
            }
            creds = tlsBuilder.build();
        }

        var launchOption = new ServerLaunchOption(option.port(), creds, Optional.of(language));
        try {
            grpcServer = Server.start(server, launchOption);
        } catch (IOException e) {
            logger.error("Failed to start grpc server");
            throw e;
        }
    }

    public static void stopApiServer() {
        if (grpcServer == null) return;
        grpcServer.stop();
        grpcServer = null;
    }

    private static void onServerStarting(MinecraftServer server) {
        RealityCommunicationMod.server = server;
        var autostartOptionPath = CONFIG_PATH.resolve("realcomm/autostart.json");
        if (!Files.exists(autostartOptionPath)) return;
        logger.info("autostart.json found");
        try {
            startApiServer(autostartOptionPath);
        } catch (Throwable e) {
            logger.error("Failed to start api server: ", e);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        stopApiServer();
    }
}
