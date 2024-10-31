plugins {
    kotlin("jvm") version "2.0.21"
    id("net.neoforged.moddev") version "2.0.42-beta"
}

neoForge {
    version = "21.0.167"

    parchment {
        minecraftVersion = "1.21"
        mappingsVersion = "2024.07.28"
    }

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create("reallink") {
            sourceSet(sourceSets.main.get())
        }
    }
}

// NeoForm's dependencies use "strictly" version. To resolve it
configurations.all {
    if (this.name in listOf("compileClasspath", "runtimeClasspath")) {
        resolutionStrategy {
            afterEvaluate {
                val minecraftCompileClasspath = configurations["neoFormRuntimeDependenciesCompileClasspath"]
                for (artifact in minecraftCompileClasspath.resolvedConfiguration.resolvedArtifacts) {
                    val id = artifact.moduleVersion.id
                    force("${id.group}:${id.name}:${id.version}")
                }
            }
        }
    }
}

dependencies {
    implementation("com.idkidknow.mcreallink:reallink-main")
    additionalRuntimeClasspath("com.idkidknow.mcreallink:reallink-main")
}
