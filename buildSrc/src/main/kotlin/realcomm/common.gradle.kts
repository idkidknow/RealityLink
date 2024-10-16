package realcomm

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

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to ModConstant.version,
        "mod_id" to ModConstant.id,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "*.mixins.json")) {
        expand(props)
    }
}
