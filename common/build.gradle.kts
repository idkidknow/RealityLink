plugins {
    id("realcomm.common")
    alias(libs.plugins.kotlin.serialization)
}

val platforms = property("enabled_platforms").toString().split(',')

architectury {
    common(platforms)
}

dependencies {
    // For mixin deps
    modImplementation(libs.fabric.loader)

    implementation(libs.kotlin.logging)
    implementation(libs.ktoml.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx.json)
}
