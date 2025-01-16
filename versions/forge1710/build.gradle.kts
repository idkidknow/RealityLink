import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask
import xyz.wagyourtail.unimined.api.runs.RunConfig

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.12"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
    maven("https://mcentral.firstdark.dev/releases")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://jitpack.io")
}

sourceSets {
    create("common")
}

unimined.minecraft(sourceSets.main.get(), sourceSets["common"]) {
    version("1.7.10")
    mappings {
        searge()
        mcp("stable", "12-1.7.10")
    }

    minecraftForge {
        loader("10.13.4.1614-1.7.10")
        mixinConfig("reallink.mixins.json")
    }

    runs {
        fun RunConfig.configAll() {
            dependsOn("writeModCoreClasspath")
            jvmArgs("-Dreallink.core.classpath=${file("run/mod-core-classpath.txt").absolutePath}")
        }
        config("client") {
            configAll()
        }
        config("server") {
            configAll()
        }
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
    "modImplementation"("com.github.LegacyModdingMC.UniMixins:unimixins-all-1.7.10:0.1.19:dev")
    "commonModImplementation"("com.github.LegacyModdingMC.UniMixins:unimixins-all-1.7.10:0.1.19:dev")
}

configurations.create("common")
artifacts {
    add("common", tasks.named<org.gradle.jvm.tasks.Jar>("commonJar").map { it.archiveFile })
}

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)

    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}

val modCore: Configuration by configurations.creating
dependencies {
    modCore(project(path = ":impl", configuration = "core"))
}
tasks.register("writeModCoreClasspath") {
    dependsOn(modCore)
    outputs.files("run/mod-core-classpath.txt")
    outputs.upToDateWhen { false }
    doLast {
        val text = modCore.resolve().map { it.absolutePath.toString() }.reduce { a, b -> "$a\n$b" }
        file("run/mod-core-classpath.txt").writeText(text)
    }
}

val modCoreRemapped: Configuration by configurations.creating
dependencies {
    modCoreRemapped(project(path = ":impl", configuration = "coreRemapped"))
}
tasks.register<Copy>("copyModCoreRemapped") {
    dependsOn(":impl:shadowJar")
    from(zipTree(modCoreRemapped.singleFile))
    into(layout.buildDirectory.dir("core_remapped/META-INF/reallink-mod-core"))
}
tasks.register<Jar>("productJar") {
    dependsOn("remapJar")
    from(zipTree(tasks.named<RemapJarTask>("remapJar").map { it.outputs.files.singleFile }))
    dependsOn("copyModCoreRemapped")
    from(layout.buildDirectory.dir("core_remapped"))
    manifest.from(tasks.jar.get().manifest)
    manifest {
        attributes(mapOf(
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "ForceLoadAsMod" to "true",
        ))
    }
    archiveClassifier = "product"
    exclude("**/*.tasty")
}
