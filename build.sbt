moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true

scalaVersion := "2.12.15"

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.0.202109080827-r",
  "com.michaelpollmeier" % "versionsort" % "1.0.11",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test)

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.11")

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
