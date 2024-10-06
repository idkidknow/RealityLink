package realcomm

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.kotlin.dsl.exclude

plugins {
    id("java")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val platform: String by project

architectury {
    platformSetupLoomIde()
}

@Suppress("UnstableApiUsage")
configurations {
    register("shadowCommon") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    register("shadowDependencies") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

dependencies {
    implementation(project(path = ":common", configuration = "namedElements"))
    "shadowCommon"(project(path = ":common", configuration = "transformProduction$platform")) {
        isTransitive = false
    }
    "shadowDependencies"(project(":common")) {
        isTransitive = true
        exclude(group = "com.idkidknow")
        exclude(group = "net.fabricmc")
    }
}

val shadowCommon = tasks.register<ShadowJar>("shadowCommon") {
    configurations = listOf(project.configurations["shadowCommon"])
    archiveClassifier = "dev-shadow-common"
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    dependsOn(shadowCommon)
    from(zipTree(shadowCommon.get().archiveFile))
    configurations = listOf(project.configurations["shadowDependencies"])
//    relocate("com", "com.idkidknow.mcrealcomm.shadow.com") {
//        exclude("com/idkidknow/mcrealcomm/**/*")
//    }
//    relocate("org", "com.idkidknow.mcrealcomm.shadow.org")
//    relocate("io", "com.idkidknow.mcrealcomm.shadow.io")
//    relocate("kotlin", "com.idkidknow.mcrealcomm.shadow.kotlin")
//    relocate("kotlinx", "com.idkidknow.mcrealcomm.shadow.kotlinx")
    archiveClassifier = "dev-shadow"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
