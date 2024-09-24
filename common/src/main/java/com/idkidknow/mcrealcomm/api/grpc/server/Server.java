package com.idkidknow.mcrealcomm.api.grpc.server;

import com.idkidknow.mcrealcomm.api.grpc.impl.ActivityServiceImpl;
import com.idkidknow.mcrealcomm.grpc.*;
import com.idkidknow.mcrealcomm.server.l10n.ServerTranslate;
import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.ChatEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
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

public class Server {
    private Server() {}

    private static final Logger logger = LogUtils.getLogger();

    private MinecraftServer minecraftServer;
    private io.grpc.Server grpcServer;
    private final ActivityServiceImpl activityService = new ActivityServiceImpl();
    private ServerLaunchOption option;

    public static Server start(MinecraftServer minecraftServer, ServerLaunchOption option) throws IOException {
        logger.info("Reality Communication grpc server starting");
        Server server = new Server();
        server.minecraftServer = minecraftServer;
        server.option = option;
        server.grpcServer = ServerBuilder.forPort(option.port()).addService(server.activityService).build().start();
        ChatEvent.RECEIVED.register(server::onChatReceived);
        PlayerEvent.PLAYER_JOIN.register(server::onPlayerJoin);
        PlayerEvent.PLAYER_QUIT.register(server::onPlayerQuit);
        PlayerEvent.PLAYER_ADVANCEMENT.register(server::onPlayerAdvancement);
        EntityEvent.LIVING_DEATH.register(server::onLivingDeath);
        return server;
    }

    public void stop() {
        activityService.shutdown();
        grpcServer.shutdown();
    }

    private EventResult onChatReceived(@Nullable ServerPlayer player, Component chatText) {
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

    private void onPlayerJoin(Player player) {
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

    private void onPlayerQuit(Player player) {
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

    private void onPlayerAdvancement(ServerPlayer player, AdvancementHolder advancement) {
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

    private EventResult onLivingDeath(LivingEntity entity, DamageSource source) {
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

    private ChatComponent transformComponent(@Nonnull Component component) {
        var json = Component.Serializer.toJson(component, minecraftServer.registryAccess());
        var builder = ChatComponent.newBuilder()
                .setJson(json);
        if (option.language().isPresent()) {
            builder.setTranslatedText(ServerTranslate.translate(component, option.language().get()));
        }
        return builder.build();
    }
}
