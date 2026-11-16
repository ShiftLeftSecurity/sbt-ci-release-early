moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true

scalaVersion := "3.9.0"

// sbt 2.1.x uses Scala 3.9.x for the metabuild, so plugins built with 3.9 are not compatible with sbt 2.0.x
pluginCrossBuild / sbtVersion := "2.1.0"

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.3.202401111512-r", // do not upgrade to 6.x unless you're willing to give up java 8 compatibility
  "com.michaelpollmeier" % "versionsort" % "1.0.14",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test)

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.2") // >= 2.3.2 is required for sbt 2.1 (2.3.1 depends on Ivy)

homepage := Some(uri("https://github.com/ShiftLeftSecurity/sbt-ci-release-early"))
scmInfo := Some(ScmInfo(
  uri("https://github.com/ShiftLeftSecurity/sbt-ci-release-early"),
  "scm:git@github.com:ShiftLeftSecurity/sbt-ci-release-early.git"))
licenses := Seq("Apache-2.0" -> uri("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "mpollmeier",
    "Michael Pollmeier",
    "michael@michaelpollmeier.com",
    uri("https://michaelpollmeier.com")
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
