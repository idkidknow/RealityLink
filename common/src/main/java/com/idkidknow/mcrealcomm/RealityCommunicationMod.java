package com.idkidknow.mcrealcomm;

import com.google.gson.Gson;
import com.idkidknow.mcrealcomm.grpc.*;
import com.idkidknow.mcrealcomm.grpc.impl.ActivityServiceImpl;
import com.idkidknow.mcrealcomm.server.l10n.ServerLanguage;
import com.idkidknow.mcrealcomm.server.l10n.ServerTranslate;
import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;

public final class RealityCommunicationMod {
    public static final String MOD_ID = "realcomm";

    private static final Logger logger = LogUtils.getLogger();
    private static MinecraftServer server;

    private static final ActivityServiceImpl activityService = new ActivityServiceImpl();
    private static Server grpcServer;

    private static Language language;

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(RealityCommunicationMod::onServerStarting);
    }

    private static void startApiServer(ServerOption option) {
        logger.info("Reality Communication API server starting");
        var resourcePacksDirStr = option.resourcePacksDir();
        var resourcePacksDir = Platform.getGameFolder().resolve(resourcePacksDirStr);
        var languageResourcePack = ServerLanguage.getResourcePackDirLanguage(resourcePacksDir, option.localeCode());
        var languageFallback = ServerLanguage.getResourceLanguage(server, option.localeCode());
        language = ServerLanguage.compose(languageResourcePack, languageFallback);
        try {
            grpcServer = ServerBuilder.forPort(option.port()).addService(activityService).build().start();
        } catch (IOException e) {
            logger.error("Failed to start grpc server: ", e);
            return;
        }
        LifecycleEvent.SERVER_STOPPING.register(RealityCommunicationMod::onServerStopping);
        ChatEvent.RECEIVED.register(RealityCommunicationMod::onChatReceived);
        PlayerEvent.PLAYER_JOIN.register(RealityCommunicationMod::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(RealityCommunicationMod::onPlayerQuit);
        PlayerEvent.PLAYER_ADVANCEMENT.register(RealityCommunicationMod::onPlayerAdvancement);
        EntityEvent.LIVING_DEATH.register(RealityCommunicationMod::onLivingDeath);
    }

    private static void onServerStarting(MinecraftServer server) {
        RealityCommunicationMod.server = server;
        var autostartOptionPath = Platform.getConfigFolder().resolve("realcomm/autostart.json");
        if (!Files.exists(autostartOptionPath)) return;
        logger.info("autostart.json found");
        try {
            var json = Files.readString(autostartOptionPath);
            var gson = new Gson();
            ServerOption option = gson.fromJson(json, ServerOption.class);
            startApiServer(option);
        } catch (IOException e) {
            logger.warn("Failed to read the option file: ", e);
        }
    }

    private static void onServerStopping(MinecraftServer server) {
        activityService.shutdown();
        grpcServer.shutdown();
    }

    private static EventResult onChatReceived(@Nullable ServerPlayer player, Component chatText) {
        if (player == null) return EventResult.pass();
        var playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
        var event = ChatReceivedEvent.newBuilder()
                .setPlayer(transformComponent(playerName))
                .setMessage(transformComponent(chatText))
                .build();
        var composedMessage = Component.translatable("chat.type.text", playerName, chatText);
        var activity = Activity.newBuilder()
                .setChatMessage(transformComponent(composedMessage))
                .setChatReceived(event)
                .build();
        activityService.sendActivity(activity);
        return EventResult.pass();
    }

    private static void onPlayerJoin(Player player) {
        var playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
        var chatMessage = Component.translatable("multiplayer.player.joined", playerName);
        var event = PlayerJoinEvent.newBuilder()
                .setPlayer(transformComponent(playerName))
                .build();
        var activity = Activity.newBuilder()
                .setChatMessage(transformComponent(chatMessage))
                .setPlayerJoin(event)
                .build();
        activityService.sendActivity(activity);
    }

    private static void onPlayerQuit(Player player) {
        var playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
        var chatMessage = Component.translatable("multiplayer.player.left", playerName);
        var event = PlayerQuitEvent.newBuilder()
                .setPlayer(transformComponent(playerName))
                .build();
        var activity = Activity.newBuilder()
                .setChatMessage(transformComponent(chatMessage))
                .setPlayerQuit(event)
                .build();
        activityService.sendActivity(activity);
    }

    private static void onPlayerAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        if (advancement.value().display().isEmpty()) return;
        var display = advancement.value().display().get();
        var playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
        var id = advancement.id().toString();
        var title = display.getTitle();
        var description = display.getDescription();
        var event = PlayerAdvancementEvent.newBuilder()
                .setPlayer(transformComponent(playerName))
                .setTitle(transformComponent(title))
                .setDescription(transformComponent(description))
                .setAdvancementId(id)
                .build();
        var chatMessage = display.getType().createAnnouncement(advancement, player);
        var activity = Activity.newBuilder()
                .setChatMessage(transformComponent(chatMessage))
                .setPlayerAdvancement(event)
                .build();
        activityService.sendActivity(activity);
    }

    private static EventResult onLivingDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Player player)) return EventResult.pass();
        var playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
        var id = source.typeHolder().getRegisteredName();
        var causingEntity = source.getEntity();
        Component causingEntityName = causingEntity == null ? null :
                (causingEntity.getDisplayName() == null ? causingEntity.getName() : causingEntity.getDisplayName());
        var directEntity = source.getDirectEntity();
        Component directEntityName = directEntity == null ? null :
                (directEntity.getDisplayName() == null ? directEntity.getName() : directEntity.getDisplayName());
        var killCredit = player.getKillCredit();
        Component killCreditName = killCredit == null ? null :
                (killCredit.getDisplayName() == null ? killCredit.getName() : killCredit.getDisplayName());
        var eventBuilder = PlayerDeathEvent.newBuilder()
                .setPlayer(transformComponent(playerName))
                .setDamageId(id);
        if (causingEntityName != null) {
            eventBuilder.setCausingEntity(transformComponent(causingEntityName));
        }
        if (directEntityName != null) {
            eventBuilder.setDirectEntity(transformComponent(directEntityName));
        }
        if (killCreditName != null) {
            eventBuilder.setKillCredit(transformComponent(killCreditName));
        }
        var event = eventBuilder.build();
        var chatMessage = source.getLocalizedDeathMessage(player);
        var activity = Activity.newBuilder()
                .setChatMessage(transformComponent(chatMessage))
                .setPlayerDeath(event)
                .build();
        activityService.sendActivity(activity);
        return EventResult.pass();
    }

    private static ChatComponent transformComponent(@Nonnull Component component) {
        var json = Component.Serializer.toJson(component, server.registryAccess());
        return ChatComponent.newBuilder()
                .setJson(json)
                .setText(component.getString())
                .setTranslatedText(ServerTranslate.translate(component, language))
                .build();
    }
}
