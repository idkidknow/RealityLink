import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RemapJarTask

plugins {
    `java-library`
    id("dev.architectury.loom") version "1.17.491"
    idea
}

group = "com.idkidknow.realitylink"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }

    withSourcesJar()
}

repositories {
    maven("https://maven.parchmentmc.org")
}

loom {
    silentMojangMappingsLicense()
    forge {
        mixinConfig("realitylink.mixins.json")
    }
}
dependencies {
    minecraft("net.minecraft:minecraft:1.16.5")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.16.5:2022.03.06@zip")
    })
    forge("net.minecraftforge:forge:1.16.5-36.2.34")
    implementation("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:1.3.6:all") // dev only
}

loom.runs {
    named("client") {
        client()
    }
    named("server") {
        server()
    }
}
val coreRunClasspathTxt = System.getenv("REALITYLINK_CORE_RUN_CLASSPATH") ?: ""
if (coreRunClasspathTxt == "") {
    for (name in listOf("runClient", "runServer")) {
        tasks.named(name).get().doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_RUN_CLASSPATH is missing")
        }
    }
} else {
    loom.runs.configureEach {
        jvmArguments.add("-Drealitylink.dev.core.classpath=$coreRunClasspathTxt")
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    description = "generate mod metadata"
    inputs.property("version", project.version)
    expand("version" to project.version)
    from("src/main/templates")
    into(layout.buildDirectory.file("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)

tasks.register("writePlatformDeps") {
    description = "command: write compileClasspath and self.jar"
    dependsOn(tasks.jar)

    val selfJar = tasks.jar.get().archiveFile.get().asFile.absolutePath
    val artifacts = configurations.compileClasspath.get().incoming.artifacts.map { it.file.absolutePath }
    val classpath = artifacts + selfJar

    val out = layout.buildDirectory.file("platform_deps.txt")
    inputs.files(classpath)
    outputs.file(out)
    doLast {
        out.get().asFile.writeText(classpath.joinToString("\n"))
    }
}

tasks.register("writePlatformSources") {
    description = "command: write compileClasspath (sources) and self-sources.jar"
    val sourcesJar = tasks.named<Jar>("sourcesJar")
    val genSourcesTask = tasks.named("genSources").get()
    dependsOn(sourcesJar, genSourcesTask)

    val selfSourcesJar = sourcesJar.get().archiveFile.get().asFile.absolutePath
    val sourcesArtifacts = configurations.compileClasspath.get().incoming.artifactView {
        @Suppress("UnstableApiUsage")
        withVariantReselection()
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }.artifacts.map { it.file.absolutePath }
    val all = sourcesArtifacts + selfSourcesJar

    val out = layout.buildDirectory.file("platform_sources.txt")
    inputs.files(all)
    outputs.file(out)
    doLast {
        out.get().asFile.writeText(all.joinToString("\n"))
    }
}

tasks.jar {
    exclude("com/idkidknow/realitylink/forge1165/ModLoadDev*.class")
}

tasks.named<RemapJarTask>("remapJar") {
    archiveFileName = "platform_thin_remapped.jar"
}

tasks.register<RemapJarTask>("remapCoreJar") {
    description = "remap core jar"
    dependsOn(tasks.compileJava)
    classpath += sourceSets.main.get().compileClasspath
    classpath += sourceSets.main.get().output
    sourceNamespace = "named"
    targetNamespace = "srg"
    archiveFileName = "core_remapped.jar"
    val coreJarFile = System.getenv("REALITYLINK_CORE_JAR") ?: ""
    if (coreJarFile == "") {
        doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_JAR is missing")
        }
    } else {
        inputFile = File(coreJarFile)
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
