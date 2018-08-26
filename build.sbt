moduleName := "sbt-ci-release"
organization := "com.michaelpollmeier"
sbtPlugin := true
version := "0.0.1" // TODO use this plugin for itself

scalaVersion := "2.12.6"
libraryDependencies ++= List(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.0.2.201807311906-r",
  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
scriptedBufferLog := false

homepage := Some(url("https://github.com/mpollmeier/sbt-ci-release"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "olafurpg",
    "Ólafur Páll Geirsson",
    "olafurpg@gmail.com",
    url("https://geirsson.com")
  ),
  Developer(
    "mpollmeier",
    "Michael Pollmeier",
    "michael@michaelpollmeier.com",
    url("https://michaelpollmeier.com")
  )
)
resolvers += Resolver.sonatypeRepo("releases")

ThisBuild/publishTo := Some("releases" at jfrog + "https://shiftleft.jfrog.io/shiftleft/libs-release-local")
