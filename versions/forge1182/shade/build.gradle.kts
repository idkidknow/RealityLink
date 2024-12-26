import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("com.gradleup.shadow")
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
    }

    relocate("com", "com/idkidknow/mcreallink/shaded/com") {
        exclude("com/idkidknow/mcreallink/**/*")
    }

    val prefixes = listOf(
        "cats",
        "toml",
//        "sttp",
        "sourcecode",
        "scodec",
        "org",
        "magnolia1",
        "io.netty",
        "geny",
//        "fs2",
        "fastparse",
//        "de",
        "buildinfo",
    )
    for (prefix in prefixes) {
        relocate(prefix, "com/idkidknow/mcreallink/shaded/$prefix")
    }

    val keywords = listOf("static", "char", "boolean", "byte", "long", "int", "short", "float", "double")
    for (keyword in keywords) {
        val relocator = SimpleRelocator(objects, keyword, "${keyword}1", null, null, true)
        relocate(relocator)
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
