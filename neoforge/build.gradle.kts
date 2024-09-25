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

    runtimeOnly(libs.grpc.netty.shaded)
    runtimeOnly(libs.grpc.protobuf)
    runtimeOnly(libs.grpc.stub)
    forgeRuntimeLibrary(libs.grpc.netty.shaded)
    forgeRuntimeLibrary(libs.grpc.protobuf)
    forgeRuntimeLibrary(libs.grpc.stub)
    include(libs.grpc.netty.shaded)
    include(libs.grpc.protobuf)
    include(libs.grpc.stub)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to version)
    }
}
