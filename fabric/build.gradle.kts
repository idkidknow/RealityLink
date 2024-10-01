plugins {
    id("realcomm.common")
    id("realcomm.platform")
}

architectury {
    fabric()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

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
    runtimeOnly(libs.ktor.serialization.kotlinx.json)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
