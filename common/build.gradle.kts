plugins {
    id("realcomm.common")
    id("com.gradleup.shadow")
    alias(libs.plugins.kotlin.serialization)
}

val platforms = property("enabled_platforms").toString().split(',')

architectury {
    common(platforms)
}

dependencies {
    // Only for mixin deps
    modImplementation(libs.fabric.loader)
}

configurations.create("commonDeps") {
    isCanBeResolved = true
    isCanBeConsumed = true
    extendsFrom(configurations.implementation.get(), configurations.runtimeOnly.get())
    afterEvaluate {
        val minecraftRuntime = configurations.named(net.fabricmc.loom.util.Constants.Configurations.MINECRAFT_RUNTIME_LIBRARIES)
        for (dep in minecraftRuntime.get().allDependencies) {
            exclude(group = dep.group, module = dep.name)
        }
    }
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.ktoml.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.netty) {
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-handler")
        exclude(group = "io.netty", module = "netty-resolver")
        exclude(group = "io.netty", module = "netty-transport-class-epoll")
        exclude(group = "io.netty", module = "transport-native-unix-common")
        exclude(group = "io.netty", module = "netty-transport")
    }
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.ktor.serialization.kotlinx.json)
}
