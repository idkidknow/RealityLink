import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import kotlin.io.path.deleteRecursively

plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.141"
    idea
}

group = "com.idkidknow.realitylink"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withSourcesJar()
}

neoForge {
    version = "21.1.235"
    validateAccessTransformers = true

    parchment {
        minecraftVersion = "1.21.1"
        mappingsVersion = "2024.11.17"
    }

    mods {
        register("realitylink") {
            sourceSet(sourceSets.main.get())
        }
    }
}

neoForge.runs {
    register("client") {
        client()
    }
    register("server") {
        server()
        programArgument("--nogui")
    }
}
val coreRunClasspathTxt = System.getenv("REALITYLINK_CORE_RUN_CLASSPATH") ?: ""
if (coreRunClasspathTxt == "") {
    for (name in listOf("runClient", "runServer", "createLaunchScripts")) {
        tasks.named(name).get().doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_RUN_CLASSPATH is missing")
        }
    }
} else {
    neoForge.runs.configureEach {
        jvmArgument("-Drealitylink.dev.core.classpath=$coreRunClasspathTxt")
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
neoForge.ideSyncTask(generateModMetadata)

tasks.register("writePlatformDeps") {
    description = "command: write compileClasspath and self.jar"
    val createMinecraftArtifacts = tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts").get()
    dependsOn(tasks.jar, createMinecraftArtifacts)

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
    val createMinecraftArtifacts = tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts").get()
    dependsOn(sourcesJar, createMinecraftArtifacts)

    val selfSourcesJar = sourcesJar.get().archiveFile.get().asFile.absolutePath
    val gameJar = createMinecraftArtifacts.gameJarArtifact.get().asFile.absolutePath
    val gameSourcesJar = createMinecraftArtifacts.gameSourcesArtifact.get().asFile.absolutePath
    val sourcesArtifacts = configurations.compileClasspath.get().incoming.artifactView {
        @Suppress("UnstableApiUsage")
        withVariantReselection()
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }.artifacts.map { it.file.absolutePath }.filter { it != gameJar }
    val all = sourcesArtifacts + listOf(selfSourcesJar, gameSourcesJar)

    val out = layout.buildDirectory.file("platform_sources.txt")
    inputs.files(all)
    outputs.file(out)
    doLast {
        out.get().asFile.writeText(all.joinToString("\n"))
    }
}

tasks.jar {
    archiveFileName = "platform_thin_remapped.jar" // no remap needed
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
