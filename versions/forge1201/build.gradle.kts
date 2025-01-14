import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    java
    id("xyz.wagyourtail.unimined") version "1.3.12"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
    version("1.20.1")
    mappings {
        mojmap()
        parchment(version = "2023.09.03")
    }

    minecraftForge {
        loader("47.3.0")
        mixinConfig("reallink.mixins.json")
    }

    runs {
        config("client") {
            dependsOn("writeModCoreClasspath")
            jvmArgs("-Dreallink.core.classpath=${file("run/mod-core-classpath.txt").absolutePath}")
        }
        config("server") {
            dependsOn("writeModCoreClasspath")
            jvmArgs("-Dreallink.core.classpath=${file("run/mod-core-classpath.txt").absolutePath}")
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
