import Dependencies._

name := "weather4s"

lazy val IntegrationTest = config("it") extend(Test)

lazy val global = (project in file("."))
  .enablePlugins(GitBranchPrompt)
  .settings(commonSettings: _*)
  .aggregate(auth, geo, loader)

lazy val auth = (project in file("auth-service"))
  .configs(IntegrationTest)
  .dependsOn(shared % "compile -> compile; test -> test; it -> it")
  // .enablePlugins(GraalVMNativeImagePlugin)
  .enablePlugins(AshScriptPlugin, DockerPlugin, JavaAppPackaging)  // Alpine -> Ash Shell
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "auth-service",
    mainClass in Compile := Some("io.sherpair.w4s.auth.Main"),
    Defaults.itSettings,
    headerSettings(IntegrationTest),
    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest)),
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= doobie ++ javamail ++ testcontainers ++ tsec
  )

lazy val geo = (project in file("geo-service"))
  .dependsOn(shared4e % "compile -> compile; test -> test")
  // .enablePlugins(GraalVMNativeImagePlugin)
  .enablePlugins(AshScriptPlugin, DockerPlugin, JavaAppPackaging)  // Alpine -> Ash Shell
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "geo-service",
    mainClass in Compile := Some("io.sherpair.w4s.geo.Main")
  )

lazy val loader = (project in file("loader-service"))
  .dependsOn(shared4e % "compile -> compile; test -> test")
  // .enablePlugins(GraalVMNativeImagePlugin)
  .enablePlugins(AshScriptPlugin, DockerPlugin, JavaAppPackaging)  // Alpine -> Ash Shell
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := "loader-service",
    mainClass in Compile := Some("io.sherpair.w4s.loader.Main")
  )

lazy val shared = (project in file("shared"))
  .settings(commonSettings: _*)

lazy val shared4e = (project in file("shared-engine"))
  .dependsOn(shared % "compile -> compile; test -> test")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= elastic ++ lucene
  )

lazy val commonSettings = Seq(
  organization := "io.sherpair",
  organizationName := "Lucio Biondi",
  startYear := Some(2019),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := "2.13.1",
  scalacOptions ++= scalacFlags,
  IntegrationTest / fork := true,
  run / fork := true,
  Test / fork := true,
  // coverageEnabled := true,
  // coverageMinimum := 80,
  // coverageFailOnMinimum := true,
  // wartremoverErrors in (Compile, compile) ++= Warts.unsafe,
  libraryDependencies ++= base ++ fs2 ++ http4s ++ jwt,
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
  // "-Ypartial-unification"   // removed for 2.13
  "-deprecation",              // warn about use of deprecated APIs
  "-encoding", "UTF-8",        // source files are in UTF-8
  "-feature",                  // warn about misused language features
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-unchecked",                // warn about unchecked type parameters
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
