plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.vanillagradle.plugin)
    implementation(libs.shadow.plugin)

    implementation("org.ow2.asm:asm:9.7.1")
}
