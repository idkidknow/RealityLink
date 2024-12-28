plugins {
    scala
    `java-library`
}

group = "com.idkidknow.mcreallink"
version = "0.2.0-SNAPSHOT"

val scalaVersion = "3.5.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.named<ScalaCompile>("compileScala") {
    scalaCompileOptions.additionalParameters = listOf(
        "-Wunused:all",
        "-P:wartremover:only-warn-traverser:org.wartremover.warts.Unsafe",
    )
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
    scalaCompilerPlugins("org.wartremover:wartremover_$scalaVersion:3.2.5")

    api("com.idkidknow.mcreallink:reallink-core-api")

    implementation("org.typelevel:cats-effect_3:3.5.5")
    implementation("org.typelevel:log4cats-slf4j_3:2.7.0")
    implementation("com.indoorvivants:toml_3:0.3.0-M2")
    implementation("io.circe:circe-core_3:0.14.10")
    implementation("io.circe:circe-generic_3:0.14.10")
    implementation("io.circe:circe-parser_3:0.14.10")
    implementation("co.fs2:fs2-core_3:3.11.0")
    implementation("co.fs2:fs2-io_3:3.11.0")
    implementation("de.lhns:fs2-compress-zip_3:2.2.1")
    implementation("com.softwaremill.sttp.tapir:tapir-core_3:1.11.9")
    implementation("com.softwaremill.sttp.tapir:tapir-json-circe_3:1.11.9")
    implementation("com.softwaremill.sttp.tapir:tapir-netty-server-cats_3:1.11.9")
    implementation("org.http4s:http4s-netty-client_3:0.5.21")
    implementation("org.http4s:http4s-circe_3:0.23.30")
}
