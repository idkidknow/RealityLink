import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.11"
    scala
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-alpha"

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

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    relocate("scala", "scala3")
//    dependencies {
//        exclude(dependency("org.scala-lang:.*"))
//    }

    relocate("org.typelevel", "com.idkidknow.mcreallink.shaded.org.typelevel")
    relocate("cats", "com.idkidknow.mcreallink.shaded.cats")
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
