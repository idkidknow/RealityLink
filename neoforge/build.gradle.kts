plugins {
    id("realcomm.minecraft")
    id("realcomm.platform")
}

architectury {
    neoForge()
}

dependencies {
    "neoForge"(libs.neoforge)
    modImplementation(libs.architectury.api.neoforge)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version)
    }
}
