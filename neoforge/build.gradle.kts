plugins {
    id("realcomm.common")
    id("realcomm.platform")
    id("realcomm.common-deps")
}

architectury {
    neoForge()
}

dependencies {
    "neoForge"(libs.neoforge)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version)
    }
}
