import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.11"
    scala
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

val scalaVersion = "3.5.2"
dependencies {
    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
    scalaCompilerPlugins("org.wartremover:wartremover_$scalaVersion:3.2.5")
}
tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf(
        "-Wunused:all",
        "-P:wartremover:only-warn-traverser:org.wartremover.warts.Unsafe",
    )
}

sourceSets {
    main {
        java {
            setSrcDirs(emptyList<String>())
            // All Java and Scala code should be placed under `main/scala`, leaving `main/java` empty.
            // Therefore, under normal circumstances, `build/classes/java/main` should not exist,
            // for which reason the game will crash in dev mode.
            //
            // However, IntelliJ may commit a "crime" by silently copying files from
            // `build/classes/scala/main` to `build/classes/java/main`. In this situation,
            // FML will complain about finding duplicate mods in dev mode.
            //
            // Solve the problems above by changing the destinationDirectory
            destinationDirectory = file(project.layout.buildDirectory.dir("classes/scala/main"))
        }
    }
}

repositories {
    mavenCentral()
    maven("https://mcentral.firstdark.dev/releases")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://jitpack.io")
}

unimined.minecraft(sourceSets.main.get()) {
    version("1.7.10")
    mappings {
        searge()
        mcp("stable", "12-1.7.10")
    }

    minecraftForge {
        loader("10.13.4.1614-1.7.10")
        mixinConfig("reallink.mixins.json")
    }

    defaultRemapJar = true
}

val shade: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    "modImplementation"("com.github.LegacyModdingMC.UniMixins:unimixins-all-1.7.10:0.1.19:dev")

    implementation(project(path = ":shade", configuration = "shadow").also { shade(it) })
    implementation("org.typelevel:cats-effect_3:3.5.5".also { shade(it) })
    implementation("org.typelevel:log4cats-core_3:2.7.0".also { shade(it) })
}

tasks.processResources {
    filesMatching("mcmod.info") {
        expand("version" to version)
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    relocate("scala", "com.idkidknow.mcreallink.shaded.scala")
//    dependencies {
//        exclude(dependency("org.scala-lang:.*"))
//    }

    relocate("org.typelevel", "com.idkidknow.mcreallink.shaded.org.typelevel")
    relocate("cats", "com.idkidknow.mcreallink.shaded.cats")
    relocate("org.slf4j", "com.idkidknow.mcreallink.shaded.org.slf4j")
    minimize()
    exclude("**/*.tasty")

    manifest {
        attributes(mapOf(
            "FMLCorePluginContainsFMLMod" to "true",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "ForceLoadAsMod" to "true",
        ))
    }
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile = shadowJar.flatMap { it.archiveFile }
}
