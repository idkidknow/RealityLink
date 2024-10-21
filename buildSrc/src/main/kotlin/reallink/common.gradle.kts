package reallink

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm")
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://repo.spongepowered.org/repository/maven-public")
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven("https://maven.parchmentmc.org/"),
            maven("https://maven.neoforged.net/releases"),
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to ModConstant.version,
        "mod_id" to ModConstant.id,
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/*.toml", "*.json")) {
        expand(props)
    }
}
