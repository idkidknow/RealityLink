package com.idkidknow.mcreallink.platform.neoforge121.impl

import com.idkidknow.mcreallink.api.BroadcastingMessageEvent
import com.idkidknow.mcreallink.api.CallingStartCommandEvent
import com.idkidknow.mcreallink.api.CallingStopCommandEvent
import com.idkidknow.mcreallink.api.ComponentClass
import com.idkidknow.mcreallink.api.Events
import com.idkidknow.mcreallink.api.LanguageClass
import com.idkidknow.mcreallink.api.MinecraftServerClass
import com.idkidknow.mcreallink.api.ServerStartingEvent
import com.idkidknow.mcreallink.api.ServerStoppingEvent
import com.idkidknow.mcreallink.api.invoke
import com.idkidknow.mcreallink.platform.Platform
import com.idkidknow.mcreallink.platform.neoforge121.mixin.complement.BroadcastingMessage
import com.idkidknow.mcreallink.platform.neoforge121.mixin.complement.ServerTranslate
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.server.MinecraftServer
import net.minecraft.util.FormattedCharSequence
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import java.io.InputStream
import java.nio.file.Path

class PlatformImpl : Platform<Component, Language, MinecraftServer> {
    override val instComponent: ComponentClass<Component, Language, MinecraftServer> = object : ComponentClass<Component, Language, MinecraftServer> {
        override fun Component.translateWith(language: Language): String =
            ServerTranslate.translate(this, language)

        override fun Component.serialize(server: MinecraftServer): String =
            Component.Serializer.toJson(this, server.registryAccess())

        override fun deserialize(json: String, server: MinecraftServer): Component =
            Component.Serializer.fromJson(json, server.registryAccess())!!

        override fun literal(text: String): Component = Component.literal(text)

    }
    override val instLanguage: LanguageClass<Language> = object : LanguageClass<Language> {
        override fun Language.get(key: String): String? = if (this.has(key)) this.getOrDefault(key) else null

        override fun create(map: (String) -> String?): Language = object : Language() {
            override fun getOrDefault(key: String, defaultValue: String): String = map(key) ?: defaultValue

            override fun has(id: String): Boolean = map(id) != null

            // only used in GUI and has no effects on server
            override fun isDefaultRightToLeft(): Boolean = false

            // only used in GUI
            override fun getVisualOrder(text: FormattedText): FormattedCharSequence = FormattedCharSequence.EMPTY
        }

        override fun parseLanguageFile(input: InputStream, output: (String, String) -> Unit) {
            Language.loadFromJson(input, output)
        }
    }

    override val instMinecraftServer: MinecraftServerClass<MinecraftServer, Component> = object : MinecraftServerClass<MinecraftServer, Component> {
        override val MinecraftServer.namespaces: Iterable<String> get() = this.resourceManager.namespaces

        override fun MinecraftServer.broadcastMessageWithoutCallback(message: Component) {
            BroadcastingMessage.ignoreTemporarily {
                this.playerList.broadcastSystemMessage(message, false)
            }
        }
    }

    private val eventsInvoker = EventsInvoker()
    override val events: Events<Component, MinecraftServer> = EventsImpl(eventsInvoker)

    init {
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStartingEvent> { event ->
            eventsInvoker.serverStarting.invoke(ServerStartingEvent(event.server))
        }
        NeoForge.EVENT_BUS.addListener<net.neoforged.neoforge.event.server.ServerStoppingEvent> { event ->
            eventsInvoker.serverStopping.invoke(ServerStoppingEvent(event.server))
        }
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            event.dispatcher.register(modCommandBuilder(
                startAction = {
                    eventsInvoker.callingStartCommand.invokeFold(CallingStartCommandEvent, Result.success(Unit), { curr, invokeNext ->
                        if (curr.isFailure) return@invokeFold curr
                        return@invokeFold invokeNext()
                    })
                },
                stopAction = {
                    eventsInvoker.callingStopCommand.invoke(CallingStopCommandEvent)
                },
            ))
        }
        BroadcastingMessage.callback = { component ->
            eventsInvoker.broadcastingMessage.invoke(BroadcastingMessageEvent(component))
        }
    }

    override val configDir: Path = FMLPaths.CONFIGDIR.get()
    override val gameDir: Path = FMLPaths.GAMEDIR.get()
    override val minecraftClassLoader: ClassLoader get() = Language::class.java.classLoader
}
