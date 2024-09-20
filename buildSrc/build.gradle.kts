plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://files.minecraftforge.net/maven/") }
    maven { url = uri("https://maven.fabricmc.net/") }
}

dependencies {
    implementation(libs.architectury.loom)
    implementation(libs.architectury.plugin)
    implementation(libs.shadow)
}
