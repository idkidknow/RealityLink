package realcomm

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

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

val shadowCommon = configurations.create("shadowCommon") {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val commonDeps = configurations.create("commonDeps")

dependencies {
    implementation(project(path = ":common", configuration = "namedElements"))
    runtimeOnly(project(":common")) // Use transitive dependencies in dev mode. Why implementation namedElements cannot work?
    afterEvaluate {
        // Architectury transformer
        "development$platform"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    }
    commonDeps(project(path = ":common", configuration = "commonDeps"))
    if (platform == "NeoForge") {
        "forgeRuntimeLibrary"(project(path = ":common", configuration = "commonDeps"))
    }
    shadowCommon(project(path = ":common", configuration = "transformProduction$platform")) {
        isTransitive = false
    }
}

val shadowCommonTask = tasks.register<ShadowJar>("shadowCommon") {
    from(sourceSets.main.get().output)
    configurations = listOf(shadowCommon)
    archiveClassifier = "dev-shadow-common"
}

commonDeps.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
    val id = artifact.moduleVersion.id
    val dep = mutableMapOf(
        "group" to id.group,
        "name" to id.name,
        "version" to id.version,
    )
    artifact.classifier?.let {
        dep["classifier"] = it
    }
    artifact.extension?.let {
        dep["ext"] = it
    }
    dependencies.add(net.fabricmc.loom.util.Constants.Configurations.INCLUDE, dep)
}

val remapJarTask = tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowCommonTask)
    inputFile.set(shadowCommonTask.flatMap { it.archiveFile })
}
