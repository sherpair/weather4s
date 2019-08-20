lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    organization := "io.sherpair",
    name := "geo-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.9",
    Defaults.itSettings,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Dependencies.root
  )

parallelExecution in IntegrationTest := false
trapExit := false

scalacOptions ++= Seq(
  "-deprecation", // warn about use of deprecated APIs
  "-encoding",
  "UTF-8", // source files are in UTF-8
  "-feature", // warn about misused language features
  "-language:higherKinds",
  "-language:postfixOps",
  "-unchecked", // warn about unchecked type parameters
  "-Xfatal-warnings",
  "-Ypartial-unification" // remove for 2.13
)
