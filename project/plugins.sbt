addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M13")
addSbtPlugin("io.get-coursier" % "sbt-shading" % "1.1.0-M13")
// addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "1.0.22")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.6")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0-M2")

// needed for sbt 1.2.8, see https://github.com/xerial/sbt-sonatype/issues/100#issuecomment-530152853
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.10"
