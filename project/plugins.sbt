addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.3.4")

// sbt  ->  dependencyBrowseGraph   dependencyStats
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")

// sbt headerCreate    sbt headerCheck
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.3.1")

// enablePlugins(JmhPlugin)
// addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.7")

// sbt <project>/docker:publishLocal    sbt <project>/docker:stage
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.5.1")

// sbt "release with-defaults"
// sbt "release release-version 1.0.99 next-version 1.2.0-SNAPSHOT"
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

// reStart  reStop  reStatus  ~reStart
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// sbt "scalafix RemoveUnusedImports"
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.11")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")

/* sbt clean coverage test (it:test)
   To generate the coverage reports -> $ sbt coverageReport */
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

// > sh <shell command>
addSbtPlugin("com.oradian.sbt" % "sbt-sh" % "0.3.0")

// sbt dependencyUpdates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0")

// addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.3")
