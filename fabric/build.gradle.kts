plugins {
    id("realcomm.common")
    id("realcomm.platform")
    id("realcomm.common-deps")
}

architectury {
    fabric()
}

dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
