plugins {
    scala
    id("xyz.wagyourtail.unimined") version "1.3.11"
}

group = "com.idkidknow.mcreallink"
version = "0.2.0"

val scalaVersion = "3.5.2"

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf(
        "-Wunused:all",
        "-P:wartremover:only-warn-traverser:org.wartremover.warts.Unsafe",
    )
}

repositories {
    mavenCentral()
}

unimined.minecraft {
    version("1.18.2")

    mappings {
        mojmap()
        parchment(version = "2022.11.06")
    }

    minecraftForge {
        loader("40.2.0")
    }
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
    scalaCompilerPlugins("org.wartremover:wartremover_$scalaVersion:3.2.5")

    implementation("com.idkidknow.mcreallink:reallink-core:$version")

    implementation("org.typelevel:log4cats-slf4j_3:2.7.0")
}
