import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import realcomm.ModConstant
import realcomm.Versions

plugins {
    id("realcomm.common")
    id("realcomm.platform")
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
    implementation(project(path = ":common", configuration = "shadedElements"))
}

val common by configurations.registering
dependencies {
    common(project(path = ":common", configuration = "shadedElements"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(common.get())
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
