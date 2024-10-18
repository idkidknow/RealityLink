package reallink

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm")
    id("com.gradleup.shadow")
}

base {
    archivesName = ModConstant.id
}

// Mixins' refmaps are coupled to the compile task
val mixinCodes by configurations.creating
val mixinResources by configurations.creating
dependencies {
    implementation(project(":mixin"))
    mixinCodes(project(path = ":mixin", configuration = "mixinCodes"))
    mixinResources(project(path = ":mixin", configuration = "mixinResources"))
}
tasks.named<ProcessResources>("processResources") {
    dependsOn(mixinResources)
    from(mixinResources)
}
tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn(mixinCodes)
    source(mixinCodes)
}
tasks.named<JavaCompile>("compileJava") {
    dependsOn(mixinCodes)
    source(mixinCodes)
}
