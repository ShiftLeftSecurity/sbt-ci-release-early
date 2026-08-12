moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true

crossScalaVersions := Seq("2.12.20", "3.8.3")
scalaVersion := "2.12.20"

(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.12.15"
    case _      => "2.0.6"
  }
}

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.3.202401111512-r", // do not upgrade to 6.x unless you're willing to give up java 8 compatibility
  "com.michaelpollmeier" % "versionsort" % "1.0.14",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test)

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")

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

ThisBuild / sonatypeCredentialHost := "central.sonatype.com"
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else sonatypePublishToBundle.value
}
