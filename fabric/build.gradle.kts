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

    runtimeOnly(libs.grpc.netty.shaded)
    runtimeOnly(libs.grpc.protobuf)
    runtimeOnly(libs.grpc.stub)
    include(libs.grpc.netty.shaded)
    include(libs.grpc.protobuf)
    include(libs.grpc.stub)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}
