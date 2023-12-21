ThisBuild / version := "0.2.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

//crossScalaVersions := Seq("2.13.12", "3.3.1")

ThisBuild / scalacOptions ++= Seq(          // use ++= to add to existing options
  "-encoding", "utf8",          // if an option takes an arg, supply it on the same line
  "-feature",                   // then put the next option on a new line for easy editing
  "-language:implicitConversions",
  "-unchecked",
  "-deprecation",
  "-Werror",
  "-Xlint",                     // exploit "trailing comma" syntax so you can add an option without editing this line
)                               // for "trailing comma", the closing paren must be on the next line

lazy val root = (project in file("."))
  .settings(
    name := "typed-future"
  )

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / organization := "dev.tayvs"
ThisBuild / githubOwner := "tayvs"
ThisBuild / githubRepository := "typed-future"
