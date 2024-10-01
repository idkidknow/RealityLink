plugins {
    id("realcomm.common")
    id("realcomm.platform")
}

architectury {
    neoForge()
}

dependencies {
    "neoForge"(libs.neoforge)

    runtimeOnly(libs.kotlin.logging)
    runtimeOnly(libs.ktoml.core)
    runtimeOnly(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.kotlinx.serialization.json)

    // ktor
    runtimeOnly(libs.ktor.server.core)
    runtimeOnly(libs.ktor.server.websockets)
    runtimeOnly(libs.ktor.server.netty) {
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-handler")
        exclude(group = "io.netty", module = "netty-resolver")
        exclude(group = "io.netty", module = "netty-transport-class-epoll")
        exclude(group = "io.netty", module = "transport-native-unix-common")
        exclude(group = "io.netty", module = "netty-transport")
    }
    "forgeRuntimeLibrary"(libs.ktor.serialization.kotlinx.json)
    "forgeRuntimeLibrary"(libs.ktor.server.core)
    "forgeRuntimeLibrary"(libs.ktor.server.websockets)
    "forgeRuntimeLibrary"(libs.ktor.server.netty) {
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-handler")
        exclude(group = "io.netty", module = "netty-resolver")
        exclude(group = "io.netty", module = "netty-transport-class-epoll")
        exclude(group = "io.netty", module = "transport-native-unix-common")
        exclude(group = "io.netty", module = "netty-transport")
    }
    "forgeRuntimeLibrary"(libs.ktor.serialization.kotlinx.json)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version)
    }
}
