import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import reallink.ModConstant
import reallink.Versions
import reallink.registerGenerateMinimalForgeMetadataAttribute

plugins {
    id("reallink.common")
    id("reallink.platform")
    alias(libs.plugins.forgegradle)
    alias(libs.plugins.librarian)
    alias(libs.plugins.mixingradle)
}

minecraft {
    mappings(mapOf(
        "channel" to "parchment",
        "version" to "${Versions.parchment}-${Versions.minecraft}",
    ))

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            mods {
                register(ModConstant.id) {
                    source(sourceSets.main.get())
                }
            }
        }
        create("server") {
            workingDirectory(project.file("run"))
            mods {
                register(ModConstant.id) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${Versions.forge}")

    annotationProcessor("org.spongepowered:mixin:${Versions.mixin}:processor")

    jarJar(libs.kotlin.stdlib) {
        isTransitive = false
        version {
            require("[2.0,3.0)")
            prefer(libs.versions.kotlin.get())
        }
    }
    minecraftLibrary(libs.kotlin.stdlib) { isTransitive = false }
}

mixin {
    add(sourceSets.main.get(), "${ModConstant.id}.refmap.json")
    config("${ModConstant.id}.mixins.json")
}

val shadowModMain by configurations.registering
val generateMetadata = registerGenerateMinimalForgeMetadataAttribute("generateMetadata") {
    modId = "${ModConstant.id}_main"
    packFormat = 15
}

dependencies {
    implementation(project(":modMain")) {
        isTransitive = false
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
    archiveClassifier = "dev"
}
tasks.jar {
    enabled = false
}
jarJar.enable()
tasks.jarJar {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().archiveFile))
    archiveClassifier = ""
    finalizedBy("reobfJar")
}
tasks.assemble { dependsOn(tasks.jarJar) }
