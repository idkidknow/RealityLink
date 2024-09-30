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
    modImplementation(libs.architectury.api.fabric)

    runtimeOnly(libs.kotlin.logging)
    runtimeOnly(libs.ktoml.core)
    runtimeOnly(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.kotlinx.serialization.json)

    // ktor
    runtimeOnly(libs.ktor.server.core)
    runtimeOnly(libs.ktor.server.websockets)
    runtimeOnly(libs.ktor.server.netty) {
        exclude(group = "io.netty")
    }
    runtimeOnly(libs.ktor.serialization.kotlinx.json)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
