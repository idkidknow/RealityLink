pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "reallink"

includeBuild("core")
includeBuild("core-api")

val versions: List<String> = file("versions").listFiles().map { f -> f.name }
versions.forEach { v -> includeBuild("versions/$v") }
