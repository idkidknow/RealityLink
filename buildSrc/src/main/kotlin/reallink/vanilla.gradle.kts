package reallink

plugins {
    id("net.neoforged.moddev")
}

neoForge {
    neoFormVersion = Versions.neoForm
    parchment {
        minecraftVersion = Versions.minecraft
        mappingsVersion = Versions.parchment
    }
}