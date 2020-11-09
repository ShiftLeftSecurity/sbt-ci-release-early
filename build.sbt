moduleName := "sbt-ci-release-early"
organization := "io.shiftleft"
sbtPlugin := true
enablePlugins(GitVersioning)

scalaVersion := "2.12.12"

libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.4.3.201909031940-r",
  "com.michaelpollmeier" % "versionsort" % "1.0.1",
  "org.apache.httpcomponents" % "httpclient" % "4.5.10",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test)

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.4")

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

Global/useGpgPinentry := true
