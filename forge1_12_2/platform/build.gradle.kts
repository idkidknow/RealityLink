import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask

plugins {
    `java-library`
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.2"
    idea
}

group = "com.idkidknow.realitylink"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }

    withSourcesJar()
}

minecraft {
    mcVersion = "1.12.2"
    injectedTags.put("VERSION", project.version)
}

tasks.injectTags {
    outputClassName = "com.idkidknow.realitylink.forge1122.Tag"
}

repositories {
    maven("https://maven.cleanroommc.com")
}

dependencies {
    modUtils.enableMixins("zone.rong:mixinbooter:11.5", "realitylink.refmap.json")
    implementation("zone.rong:mixinbooter:11.5") {
        isTransitive = false
    }
    annotationProcessor("zone.rong:mixinbooter:11.5")
    implementation("xyz.wagyourtail.jvmdowngrader:jvmdowngrader:1.3.6:all") // dev only
}

val coreRunClasspathTxt = System.getenv("REALITYLINK_CORE_RUN_CLASSPATH") ?: ""
if (coreRunClasspathTxt == "") {
    for (name in listOf("runClient", "runServer")) {
        tasks.named(name).get().doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_RUN_CLASSPATH is missing")
        }
    }
} else {
    minecraft.extraRunJvmArguments.addAll(
        "-Drealitylink.dev.core.classpath=$coreRunClasspathTxt",
        "-DLWJGL_DISABLE_XRANDR=true",
        "-Dmixin.hotSwap=true",
        "-Dmixin.checks.interfaces=true",
        "-Dmixin.debug.export=true",
    )
    tasks.withType<RunMinecraftTask>().configureEach {
        extraArgs.addAll("--mixin", "realitylink.mixins.json")
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

configurations {
    register("platform") {
        extendsFrom(compileClasspath.get())
        exclude(group = "org.scala-lang")
    }
}

tasks.register("writePlatformDeps") {
    description = "command: write compileClasspath and self.jar"
    dependsOn(tasks.jar, tasks.packagePatchedMc)

    val selfJar = tasks.jar.get().archiveFile.get().asFile.absolutePath
    val mcJar = tasks.packagePatchedMc.get().archiveFile.get().asFile.absolutePath
    val artifacts = configurations.named("platform").get().incoming.artifacts.map { it.file.absolutePath }
    val classpath = artifacts + selfJar + mcJar

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
    dependsOn(sourcesJar, tasks.remapDecompiledJar)

    val selfSourcesJar = sourcesJar.get().archiveFile.get().asFile.absolutePath
    val mcSourcesJar = tasks.remapDecompiledJar.get().outputJar.asFile.get().absolutePath
    val sourcesArtifacts = configurations.named("platform").get().incoming.artifactView {
        @Suppress("UnstableApiUsage")
        withVariantReselection()
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }.artifacts.map { it.file.absolutePath }
    val all = sourcesArtifacts + selfSourcesJar + mcSourcesJar

    val out = layout.buildDirectory.file("platform_sources.txt")
    inputs.files(all)
    outputs.file(out)
    doLast {
        out.get().asFile.writeText(all.joinToString("\n"))
    }
}

tasks.register<Jar>("remapJar") {
    description = "reobfJar for final packaging"
    from(zipTree(tasks.named<ReobfuscatedJar>("reobfJar").flatMap { it.archiveFile }))
    exclude("com/idkidknow/realitylink/forge1122/ModLoadDev*.class")
    archiveFileName = "platform_thin_remapped.jar"

    manifest {
        attributes(
            "FMLCorePluginContainsFMLMod" to true,
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "realitylink.mixins.json",
        )
    }
}

tasks.register<ReobfuscatedJar>("remapCoreJar") {
    description = "remap core jar"
    dependsOn(tasks.compileJava, tasks.generateForgeSrgMappings, tasks.packagePatchedMc)
    referenceClasspath = sourceSets.main.get().runtimeClasspath
    mcVersion = "1.12.2"
    srg = tasks.generateForgeSrgMappings.flatMap { it.mcpToSrg }
    fieldCsv = tasks.generateForgeSrgMappings.flatMap { it.fieldsCsv }
    methodCsv = tasks.generateForgeSrgMappings.flatMap { it.methodsCsv }
    exceptorCfg = tasks.generateForgeSrgMappings.flatMap { it.srgExc }
    recompMcJar = tasks.packagePatchedMc.flatMap { it.archiveFile }
    archiveFileName = "core_remapped.jar"
    val coreJarFile = System.getenv("REALITYLINK_CORE_JAR") ?: ""
    if (coreJarFile == "") {
        doFirst {
            throw GradleException("Environment variable REALITYLINK_CORE_JAR is missing")
        }
    } else {
        inputJar = File(coreJarFile)
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
