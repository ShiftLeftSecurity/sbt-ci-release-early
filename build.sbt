moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true

scalaVersion := "2.12.20"

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.3.202401111512-r", // do not upgrade to 6.x unless you're willing to give up java 8 compatibility
  "com.michaelpollmeier" % "versionsort" % "1.0.11",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test)

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.0")
// TODO back to official release once our PR is merged and released:
// https://github.com/xerial/sbt-sonatype/pull/591
// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.michaelpollmeier" % "sbt-sonatype" % "3.12.2-18-2c83e453") 
// https://github.com/mpollmeier/sbt-sonatype/tree/michael/custom-release
// https://github.com/mpollmeier/sbt-sonatype/commit/2c83e4535a122d001c5d184087b00288a52ed817

homepage := Some(url("https://github.com/ShiftLeftSecurity/sbt-ci-release-early"))
scmInfo := Some(ScmInfo(
  url("https://github.com/ShiftLeftSecurity/sbt-ci-release-early"),
  "scm:git@github.com:ShiftLeftSecurity/sbt-ci-release-early.git"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "mpollmeier",
    "Michael Pollmeier",
    "michael@michaelpollmeier.com",
    url("https://michaelpollmeier.com")
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

publishTo := sonatypePublishToBundle.value
