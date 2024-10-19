package reallink

plugins {
    id("java")
    id("org.spongepowered.gradle.vanilla")
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

minecraft {
    version(Versions.minecraft)
}
