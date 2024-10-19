plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.vanillagradle.plugin)
    implementation(libs.shadow.plugin)
}
