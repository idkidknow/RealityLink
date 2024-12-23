import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.idkidknow.mcreallink:reallink-core")
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()

    dependencies {
        exclude(dependency("org.scala-lang:.*"))
        exclude(dependency("org.typelevel:cats.*"))
    }

    relocate("com", "com/idkidknow/mcreallink/shaded/com") {
        exclude("com/idkidknow/mcreallink/**/*")
    }

    val prefixes = listOf(
        "toml",
//        "sttp",
        "sourcecode",
        "scodec",
        "org/http4s",
        "org/playframework",
        "org/reactivestreams",
        "magnolia1",
        "io.netty",
        "geny",
//        "fs2",
        "fastparse",
        "de/lhns/fs2",
        "buildinfo",
    )
    for (prefix in prefixes) {
        relocate(prefix, "com/idkidknow/mcreallink/shaded/$prefix")
    }

    val excludedPatterns = listOf(
        "*.conf",
        "*.txt",
        "LICENSE",
        "CONTRIBUTING",
        "NOTICE",
    )
    excludedPatterns.forEach { exclude(it) }
}
