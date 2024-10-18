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
configurations.register("shadedElements") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

shade.exclude(group = "org.jetbrains.kotlin")
shade.exclude(group = "org.slf4j", module = "slf4j-api")

// NeoForm's dependencies use "strictly" version. To resolve it
configurations.all {
    if (this.name in listOf("compileClasspath", "runtimeClasspath")) {
        resolutionStrategy {
            afterEvaluate {
                val minecraftCompileClasspath = configurations["neoFormRuntimeDependenciesCompileClasspath"]
                for (artifact in minecraftCompileClasspath.resolvedConfiguration.resolvedArtifacts) {
                    val id = artifact.moduleVersion.id
                    force("${id.group}:${id.name}:${id.version}")
                }
            }
        }
    }
}

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
