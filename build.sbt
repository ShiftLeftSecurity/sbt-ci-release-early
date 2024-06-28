moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true

scalaVersion := "2.12.17"

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.1.202206130422-r", // do not upgrade to 6.x unless you're willing to give up java 8 compatibility
  "com.michaelpollmeier" % "versionsort" % "1.0.11",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test)

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.0.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.0")

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
