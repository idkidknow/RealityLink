ThisBuild / scalaVersion := "3.5.2"
ThisBuild / organization := "com.idkidknow"

lazy val root = (project in file("."))
 .settings(
    name := "reallink-core",
    version := "0.2.0",
    scalacOptions += "-Wunused:all",
    wartremoverWarnings ++= Warts.all,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.5",
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "com.indoorvivants" %% "toml" % "0.3.0-M2",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "co.fs2" %% "fs2-core" % "3.11.0",
      "co.fs2" %% "fs2-io" % "3.11.0",
      "de.lhns" %% "fs2-compress-zip" % "2.2.1",
    ),
 )
