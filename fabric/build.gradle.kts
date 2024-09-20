plugins {
    id("realcomm.minecraft")
    id("realcomm.platform")
}

architectury {
    fabric()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.architectury.api.fabric)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
