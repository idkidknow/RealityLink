plugins {
    scala
}

group = "com.idkidknow.mcreallink"
version = "0.2.0"

val scalaVersion = "3.6.1"

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf(
        "-Wunused:all",
        "-P:wartremover:only-warn-traverser:org.wartremover.warts.All",
    )
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
    scalaCompilerPlugins("org.wartremover:wartremover_$scalaVersion:3.2.4")

    implementation("org.typelevel:cats-effect_3:3.5.5")
    implementation("org.typelevel:log4cats-slf4j_3:2.7.0")
    implementation("com.indoorvivants:toml_3:0.3.0-M2")
    implementation("io.circe:circe-core_3:0.14.10")
    implementation("io.circe:circe-generic_3:0.14.10")
    implementation("co.fs2:fs2-core_3:3.11.0")
    implementation("co.fs2:fs2-io_3:3.11.0")
    implementation("de.lhns:fs2-compress-zip_3:2.2.1")
}
