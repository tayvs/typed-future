ThisBuild / version := "0.3.1-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

crossScalaVersions := Seq("2.13.14", "3.3.3")

lazy val root = (project in file("."))
  .settings(
    name := "typed-future",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    scalacOptions ++= Seq(          // use ++= to add to existing options
      "-encoding", "utf8",          // if an option takes an arg, supply it on the same line
      "-feature",                   // then put the next option on a new line for easy editing
      "-language:implicitConversions",
      "-unchecked",
      "-deprecation",
      "-Werror",
      "-Xlint",                     // exploit "trailing comma" syntax so you can add an option without editing this line
    )                               // for "trailing comma", the closing paren must be on the next line
  )

lazy val `scala3-test` = (project in file("scala3-test"))
  .dependsOn(root)
  .settings(
    name := "scala3-tests",
    scalaVersion := "3.3.3",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-feature",
      "-language:implicitConversions",
      "-unchecked",
      "-deprecation",
      "-Werror",
    )
  )

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / organization := "dev.tayvs"
ThisBuild / githubOwner := "tayvs"
ThisBuild / githubRepository := "typed-future"
