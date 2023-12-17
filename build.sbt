version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "TypedFuture"
  )

//organization := "org.name"

//versionScheme := Some("semver-spec")
//
//homepage := Some(url("https://github.com/tayvs/TypedFuture"))
////licenses := Seq("LICENSE" -> url("LICENSE_URL"))
//publishMavenStyle := true
//pomIncludeRepository := { _ => true }

githubOwner := "tayvs"
githubRepository := "TypedFuture"

resolvers += Resolver.githubPackages("tayvs"/*, "TypedFuture"*/)

publishMavenStyle := true
pomIncludeRepository := { _ => false }
githubTokenSource := TokenSource.Or(TokenSource.Environment("GITHUB_TOKEN"), TokenSource.GitConfig("github.packages-token"))
githubSuppressPublicationWarning := false

publishTo := Some("GitHub tayvs Apache Maven Packages" at "https://maven.pkg.github.com/tayvs/TypedFuture")
//credentials += Credentials(
//  "GitHub Package Registry",
//  "maven.pkg.github.com",
//  "tayvs",
//  System.getenv("GITHUB_TOKEN")
//)
