import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import reallink.ModConstant
import reallink.Versions

plugins {
    id("reallink.common")
    id("reallink.platform")
    id("net.neoforged.moddev")
}

neoForge {
    version = Versions.neoForge

    parchment {
        minecraftVersion = Versions.minecraft
        mappingsVersion = Versions.parchment
    }

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
    }

    mods {
        register(ModConstant.id) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    jarJar(libs.kotlin.stdlib) {
        version {
            require("[2.0, 3.0)")
            prefer(libs.versions.kotlin.get())
        }
    }
    additionalRuntimeClasspath(libs.kotlin.stdlib)
    implementation(project(path = ":modMain", configuration = "shadedElements"))
}

val shadowModMain by configurations.registering
dependencies {
    shadowModMain(project(path = ":modMain", configuration = "shadedElements"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowModMain.get())
    archiveClassifier = "no-jar-jar"
}
tasks.jar {
    enabled = false
}
val mergeJar = tasks.register<Jar>("mergeJar") {
    dependsOn(shadowJar)
    from(zipTree(shadowJar.get().archiveFile))
    from(tasks.named("jarJar"))
    archiveClassifier = null
}
tasks.assemble {
    dependsOn(mergeJar)
}
