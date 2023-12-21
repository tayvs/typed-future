version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "typed-future"
  )

organization := "dev.tayvs"
githubOwner := "tayvs"
githubRepository := "typed-future"
