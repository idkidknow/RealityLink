import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("reallink.common")
    id("reallink.vanilla")
    id("com.gradleup.shadow")
    alias(libs.plugins.kotlin.serialization)
}

val shade by configurations.creating {
    isTransitive = true
    isCanBeResolved = true
    isCanBeConsumed = false
}
val internal by configurations.creating
configurations.compileClasspath { extendsFrom(internal) }
configurations.runtimeClasspath { extendsFrom(internal) }

shade.exclude(group = "org.jetbrains.kotlin")
shade.exclude(group = "org.slf4j", module = "slf4j-api")

dependencies {
    compileOnly(libs.mixin)

    libs.kotlin.logging.also { shade(it) }.also { internal(it) }
    libs.ktoml.core.also { shade(it) }.also { internal(it) }
    libs.kotlinx.coroutines.core.also { shade(it) }.also { internal(it) }
    libs.kotlinx.serialization.json.also { shade(it) }.also { internal(it) }

    // ktor
    libs.ktor.server.core.also { shade(it) }.also { internal(it) }
    libs.ktor.server.websockets.also { shade(it) }.also { internal(it) }
    libs.ktor.server.netty.also { shade(it) }.also { internal(it) }
    libs.ktor.network.tls.certificates.also { shade(it) }.also { internal(it) }
    libs.ktor.serialization.kotlinx.json.also { shade(it) }.also { internal(it) }
}

configurations.register("shadedElements") {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
}
val shadeTask = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    archiveClassifier = "dev"
    isEnableRelocation = true
    relocationPrefix = "com.idkidknow.mcreallink.shaded"
    mergeServiceFiles()
    minimize()
}

artifacts {
    add("shadedElements", shadeTask)
}
