import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.11"
    scala
    id("com.gradleup.shadow")
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
}

unimined.minecraft(sourceSets.main.get()) {
    version("1.18.2")
    mappings {
        mojmap()
        parchment(version = "2022.11.06")
    }

    minecraftForge {
        loader("40.2.0")
        mixinConfig("reallink.mixins.json")
    }

    minecraftLibraries.dependencies.add(project.dependencies.project(path = ":shade", configuration = "shadow"))

    defaultRemapJar = true
}

val shade: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(project(path = ":shade", configuration = "shadow").also { shade(it) })
}

tasks.processResources {
    filesMatching("META-INF/mods.toml") {
        expand("version" to version)
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    relocate("scala", "com.idkidknow.mcreallink.shaded.scala")
//    dependencies {
//        exclude(dependency("org.scala-lang:.*"))
//    }

//    relocate("org.typelevel", "com.idkidknow.mcreallink.shaded.org.typelevel")
//    relocate("cats", "com.idkidknow.mcreallink.shaded.cats")
//    relocate("org.slf4j", "com.idkidknow.mcreallink.shaded.org.slf4j")
    minimize()
    exclude("**/*.tasty")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile = shadowJar.flatMap { it.archiveFile }
}
