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
configurations.register("shadedElements") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

shade.exclude(group = "org.jetbrains.kotlin")

dependencies {
    compileOnly(libs.mixin)

    implementation(libs.kotlin.logging.also { shade(it) })
    implementation(libs.ktoml.core.also { shade(it) })
    implementation(libs.kotlinx.coroutines.core.also { shade(it) })
    implementation(libs.kotlinx.serialization.json.also { shade(it) })

    // ktor
    implementation(libs.ktor.server.core.also { shade(it) })
    implementation(libs.ktor.server.websockets.also { shade(it) })
    implementation(libs.ktor.server.netty.also { shade(it) })
    implementation(libs.ktor.network.tls.certificates.also { shade(it) })
    implementation(libs.ktor.serialization.kotlinx.json.also { shade(it) })

    // Minecraft 1.16.5 uses Log4j 2.8.1 but there's no such thing as
    // a bridge between newest SLF4J and Log4j 2.8.1 (log4j-slf4j2-impl:2.8.1)
    // so we use slf4j-simple here as a workaround
    //
    // defect: All logs are sent to stderr and shown with a verbose prefix in Minecraft's log.
    // Logging level defaults to INFO and is not easy to change
    implementation("org.slf4j:slf4j-simple:2.0.16".also { shade(it) })
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    archiveClassifier = "dev"
    isEnableRelocation = true
    relocationPrefix = "com.idkidknow.mcreallink.shaded"
    mergeServiceFiles()
    minimize()
}

artifacts {
    add("shadedElements", shadowJar)
}
