plugins {
    id("realcomm.common")
    alias(libs.plugins.kotlin.serialization)
}

val platforms = property("enabled_platforms").toString().split(',')

architectury {
    common(platforms)
}

dependencies {
    // We depend on Fabric Loader here to use the Fabric @Environment annotations,
    // which get remapped to the correct annotations on each platform.
    // Do NOT use other classes from Fabric Loader.
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
