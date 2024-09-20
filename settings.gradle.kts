rootProject.name = "RealityCommunication"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://files.minecraftforge.net/maven/") }
        maven { url = uri("https://maven.fabricmc.net/") }
    }
}

include("common")
include("neoforge")
include("fabric")
