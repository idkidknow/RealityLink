import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.12"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
    maven("https://mcentral.firstdark.dev/releases")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.cleanroommc.com")
}

sourceSets {
    create("common")
}

unimined.minecraft(sourceSets.main.get(), sourceSets["common"]) {
    version("1.12.2")
    mappings {
        searge()
        mcp("stable", "39-1.12")
    }

    runs {
        config("client") {
            jvmArgs("-Dfml.coreMods.load=zone.rong.mixinbooter.MixinBooterPlugin")
            args("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
        config("server") {
            jvmArgs("-Dfml.coreMods.load=zone.rong.mixinbooter.MixinBooterPlugin")
            args("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }

    minecraftForge {
        loader("14.23.5.2860")
        mixinConfig("reallink.mixins.json")
    }

    if (sourceSet == sourceSets.main.get()) {
        combineWith(sourceSets["common"])
    } else {
        runs {
            off = true
        }
    }
}

dependencies {
    "modImplementation"("zone.rong:mixinbooter:10.2")
    "commonModImplementation"("zone.rong:mixinbooter:10.2")
}

configurations.create("common")
artifacts {
    add("common", tasks.named<org.gradle.jvm.tasks.Jar>("commonJar").map { it.archiveFile })
}

val modCore: Configuration by configurations.creating
val modCoreRemapped: Configuration by configurations.creating
dependencies {
    modCore(project(path = ":impl", configuration = "core"))
    modCoreRemapped(project(path = ":impl", configuration = "coreRemapped"))
}
tasks.register<Copy>("copyModCore") {
    dependsOn(":impl:shadowJar")
    from(zipTree(modCore.singleFile))
    into(layout.buildDirectory.dir("generated_core/META-INF/mod-core"))
}
tasks.register<Copy>("copyModCoreRemapped") {
    dependsOn(":impl:remapShadowJar")
    from(zipTree(modCoreRemapped.singleFile))
    into(layout.buildDirectory.dir("generated_core_remapped/META-INF/mod-core"))
}
tasks.processResources {
    dependsOn("copyModCore")
}
sourceSets.main.get().resources.srcDir(layout.buildDirectory.dir("generated_core"))

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)

    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}

tasks.jar {
    exclude("*.tasty")
    exclude("META-INF/mod-core/**/*")
}
tasks.register<Jar>("productJar") {
    dependsOn("remapJar")
    from(zipTree(tasks.named<RemapJarTask>("remapJar").map { it.outputs.files.singleFile }))
    dependsOn("copyModCoreRemapped")
    from(layout.buildDirectory.dir("generated_core_remapped"))
    manifest.from(tasks.jar.get().manifest)
    manifest {
        attributes(mapOf(
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "ForceLoadAsMod" to "true",
        ))
    }
    archiveClassifier = "product"
}
