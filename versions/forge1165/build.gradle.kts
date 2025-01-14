import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask
import xyz.wagyourtail.unimined.api.runs.RunConfig

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
}

sourceSets {
    create("common")
}

unimined.minecraft(sourceSets.main.get(), sourceSets["common"]) {
    version("1.16.5")
    mappings {
        searge()
        mojmap()
        parchment(version = "2022.03.06")
    }

    minecraftForge {
        loader("36.2.34")
        mixinConfig("reallink.mixins.json")
    }

    runs {
        fun RunConfig.configAll() {
            dependsOn("writeModCoreClasspath")
            jvmArgs("-Dreallink.core.classpath=${file("run/mod-core-classpath.txt").absolutePath}")

            // `properties["source_roots"]` will be passed as the environment variable `MOD_CLASSES`
            // and Forge in 1.16.5 will try to read mods.toml in the **first** path. But
            // unimined put `resources/common` first (expect `resources/main`)
            fun rearrangeSourceRoots(sourceRootsStr: String): String {
                val sourceRoots = sourceRootsStr.split(File.pathSeparator)
                val mainRes = sourceRoots.find { s -> s.contains(sourceSets.main.get().output.resourcesDir.toString()) }!!
                val newSourceRoots = mutableListOf(mainRes)
                newSourceRoots.addAll(sourceRoots.filter { s -> s != mainRes })
                return newSourceRoots.joinToString(File.pathSeparator)
            }
            val oldSourceRoots = this.properties["source_roots"]!!
            this.properties["source_roots"] = {
                val sourceRootsStr = oldSourceRoots.invoke()
                rearrangeSourceRoots(sourceRootsStr)
            }
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

configurations.create("common")
artifacts {
    add("common", tasks.named<org.gradle.jvm.tasks.Jar>("commonJar").map { it.archiveFile })
}

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
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
    archiveClassifier = "product"
    exclude("**/*.tasty")
}

