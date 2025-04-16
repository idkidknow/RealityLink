import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.12"
    scala
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = rootProject.group
version = rootProject.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven("https://mcentral.firstdark.dev/releases")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.idkidknow.com/releases")
}

// scala
val scalaVersion = "3.6.2"
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

unimined.minecraft(sourceSets.main.get()) {
    version("1.20.1")
    mappings {
        mojmap()
        parchment(version = "2023.09.03")
    }

    minecraftForge {
        loader("47.3.0")
    }

    runs {
        off = true
    }
}

dependencies {
    compileOnly(project(path = ":" , configuration = "common"))
    implementation("com.idkidknow.mcreallink:reallink-core_3:0.2.0")
}

val shade: Configuration by configurations.creating
dependencies {
    shade("com.idkidknow.mcreallink:reallink-core_3")
}
tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)

    dependencies {
        exclude(dependency("org.slf4j:.*"))
    }

    dependsOn("remapJar")
    from(zipTree(tasks.named<RemapJarTask>("remapJar").map { it.outputs.files.singleFile }))
    archiveClassifier = "all"
    minimize()
}

configurations.create("core")
dependencies {
    "core"("com.idkidknow.mcreallink:reallink-core_3")
}
configurations.create("coreRemapped")
artifacts {
    add("core", tasks.jar)
    add("coreRemapped", tasks.named<ShadowJar>("shadowJar").map { it.archiveFile })
}
