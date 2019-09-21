import Dependencies._

name := "weather4s"

lazy val global = (project in file("."))
  .enablePlugins(GitBranchPrompt)
  .settings(commonSettings: _*)
  .aggregate(authService, geoService, loaderService)

lazy val authService = (project in file("auth-service"))
  .configs(IntegrationTest)
  // .enablePlugins(DockerPlugin, GraalVMNativeImagePlugin)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "auth-service",
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= fs2 ++ http4s
  )

lazy val domain = project
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= elastic ++ lucene
  )

lazy val geoService = (project in file("geo-service"))
  .configs(IntegrationTest)
  .dependsOn(domain % "compile -> compile; test -> test")
  // .enablePlugins(GraalVMNativeImagePlugin)
  .enablePlugins(AshScriptPlugin, DockerPlugin, JavaAppPackaging)  // Alpine -> Ash Shell
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "geo-service",
    mainClass in Compile := Some("io.sherpair.w4s.geo.Main"),
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= fs2 ++ http4s ++ http4sClient
  )

lazy val loaderService = (project in file("loader-service"))
  .configs(IntegrationTest)
  .dependsOn(domain % "compile -> compile; test -> test")
  // .enablePlugins(DockerPlugin, GraalVMNativeImagePlugin)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "loader-service",
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= fs2 ++ http4s
  )

lazy val commonSettings = Seq(
  organization := "io.sherpair",
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scalaVersion := "2.13.1",
  scalacOptions ++= scalacFlags,
  // coverageEnabled := true,
  // coverageMinimum := 80,
  // coverageFailOnMinimum := true,
  // wartremoverErrors in (Compile, compile) ++= Warts.unsafe,
  libraryDependencies ++= base,
  // trapExit := false,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  scmInfo := Some(ScmInfo(url("https://github.com/sherpair/weather4s"), "scm:git:git@github.com:sherpair/weather4s.git"))
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "openjdk:8-jre-alpine",
  // dockerRepository := Some("hub.docker.com")
  dockerUpdateLatest := true
)

lazy val scalacFlags = Seq(
  // "-Ypartial-unification"   // remove for 2.13
  "-deprecation", // warn about use of deprecated APIs
  "-encoding",
  "UTF-8", // source files are in UTF-8
  "-feature", // warn about misused language features
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-unchecked", // warn about unchecked type parameters
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfatal-warnings"
)

addCommandAlias("cov", "all clean coverage test it:test")
addCommandAlias("dock", "all docker:publishLocal")
addCommandAlias("fix", "all compile:scalafix test:scalafix it:scalafmt")
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("graal", "show graalvm-native-image:packageBin")
addCommandAlias("rel", "all release with-defaults")
addCommandAlias("upd", "all dependencyUpdates")
