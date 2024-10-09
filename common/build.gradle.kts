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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.javalin)
    implementation(libs.javalin.ssl.plugin)
}
