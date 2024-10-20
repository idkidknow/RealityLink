import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import reallink.ModConstant
import reallink.Versions
import reallink.registerGenerateMinimalNeoForgeMetadataAttribute

plugins {
    id("reallink.common")
    id("reallink.platform")
    id("net.neoforged.moddev")
}

neoForge {
    version = Versions.neoForge

    parchment {
        minecraftVersion = Versions.minecraft
        mappingsVersion = Versions.parchment
    }

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
    }

    mods {
        register(ModConstant.id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    jarJar(libs.kotlin.stdlib) {
        version {
            require("[2.0, 3.0)")
            prefer(libs.versions.kotlin.get())
        }
    }
    additionalRuntimeClasspath(libs.kotlin.stdlib)
}

val shadowModMain by configurations.registering
val generateMetadata = registerGenerateMinimalNeoForgeMetadataAttribute("generateMetadata") {
    modId = "${ModConstant.id}_main"
}
dependencies {
    implementation(project(":modMain")) {
        attributes {
            attribute(generateMetadata, true)
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
    shadowModMain(project(":modMain")) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowModMain.get())
    archiveClassifier = "no-jar-jar"
}
tasks.jar {
    enabled = false
}
val mergeJar = tasks.register<Jar>("mergeJar") {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().archiveFile))
    from(tasks.named("jarJar"))
    archiveClassifier = null
}
tasks.assemble {
    dependsOn(mergeJar)
}
