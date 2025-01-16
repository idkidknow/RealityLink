plugins {
    scala
    `java-library`
    `maven-publish`
}

group = "com.idkidknow.mcreallink"
version = "0.2.0"

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
}

publishing {
    repositories {
        maven {
            name = "myRepo"
            url = uri("https://maven.idkidknow.com/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "${artifactId}_3"
        }
    }
}
