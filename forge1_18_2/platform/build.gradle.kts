import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar
import net.neoforged.nfrtgradle.CreateMinecraftArtifacts

plugins {
    `java-library`
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
    idea
}

group = "com.idkidknow.realitylink"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    withSourcesJar()
}

legacyForge {
    version = "1.18.2-40.3.0"
    validateAccessTransformers = true

    parchment {
        minecraftVersion = "1.18.2"
        mappingsVersion = "2022.11.06"
    }

    mods {
        register("realitylink") {
            sourceSet(sourceSets.main.get())
        }
    }
}

legacyForge.runs {
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
    legacyForge.runs.configureEach {
        jvmArgument("-Drealitylink.dev.core.classpath=$coreRunClasspathTxt")
    }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}
mixin {
    add(sourceSets.main.get(), "realitylink.refmap.json")
    config("realitylink.mixins.json")
}
tasks.jar {
    manifest.attributes("MixinConfigs" to "realitylink.mixins.json")
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    description = "generate mod metadata"
    inputs.property("version", project.version)
    expand("version" to project.version)
    from("src/main/templates")
    into(layout.buildDirectory.file("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
legacyForge.ideSyncTask(generateModMetadata)

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

tasks.named<RemapJar>("reobfJar") {
    archiveFileName = "platform_thin_remapped.jar"
}

sourceSets {
    // only used in `obfuscation.reobfuscate` to just provide compileClasspath
    register("dummy") {
        compileClasspath += sourceSets.main.get().output
        compileClasspath += sourceSets.main.get().compileClasspath
    }
}
java.registerFeature("dummy") {
    usingSourceSet(sourceSets.named("dummy").get())
}
val coreJar = tasks.register<Jar>("coreJar") {
    description = "copy from `core` and remap it later"
    dependsOn(tasks.compileJava) // require `sourceSets.main.get().output` to remap
    val coreJarFile = System.getenv("REALITYLINK_CORE_JAR") ?: ""
    if (coreJarFile == "") {
        doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_JAR is missing")
        }
    } else {
        inputs.files(coreJarFile)
        from(zipTree(coreJarFile))
        archiveFileName = "core.jar"
    }
}
obfuscation.reobfuscate(coreJar, sourceSets.named("dummy").get())
tasks.named<RemapJar>("reobfCoreJar") {
    archiveFileName = "core_remapped.jar"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
