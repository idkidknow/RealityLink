package realcomm

plugins {
    id("dev.architectury.loom")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-1940357260
val libs = versionCatalogs.named("libs")
fun findLibrary(alias: String): Provider<*> = libs.findLibrary(alias).get()

val commonImplementation = configurations.create("commonImplementation")
configurations.implementation.get().extendsFrom(commonImplementation)
if (project.hasProperty("platform") && project.property("platform") == "NeoForge") {
    configurations.get("forgeRuntimeLibrary").extendsFrom(commonImplementation)
}

dependencies {
    commonImplementation(findLibrary("kotlin-logging"))
    commonImplementation(findLibrary("ktoml-core"))
    commonImplementation(findLibrary("kotlinx-coroutines-core"))
    commonImplementation(findLibrary("kotlinx-serialization-json"))

    // ktor
    commonImplementation(findLibrary("ktor-server-core"))
    commonImplementation(findLibrary("ktor-server-websockets"))
    commonImplementation(findLibrary("ktor-server-netty")) {
        exclude(group = "io.netty", module = "netty-buffer")
        exclude(group = "io.netty", module = "netty-codec")
        exclude(group = "io.netty", module = "netty-common")
        exclude(group = "io.netty", module = "netty-handler")
        exclude(group = "io.netty", module = "netty-resolver")
        exclude(group = "io.netty", module = "netty-transport-class-epoll")
        exclude(group = "io.netty", module = "transport-native-unix-common")
        exclude(group = "io.netty", module = "netty-transport")
    }
    commonImplementation(findLibrary("ktor-network-tls-certificates"))
    commonImplementation(findLibrary("ktor-serialization-kotlinx-json"))
}

