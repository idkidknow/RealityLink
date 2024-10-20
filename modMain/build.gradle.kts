import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import reallink.Versions

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

    // Minecraft 1.16.5 uses Log4j 2.8.1 but there's no such thing as
    // a bridge between newest SLF4J and Log4j 2.8.1 (log4j-slf4j2-impl:2.8.1)
    // so we use slf4j-simple here as a workaround
    //
    // defect: All logs are sent to stderr and shown with a verbose prefix in Minecraft's log.
    // Logging level defaults to INFO and is not easy to change
    "org.slf4j:slf4j-simple:2.0.16".also { shade(it) }.also { internal(it) }
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
