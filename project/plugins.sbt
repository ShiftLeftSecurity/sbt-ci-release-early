addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "2.1.15")

// must be >= 2.3.2 for sbt 2.1 (the 2.3.1 that comes in transitively via sbt-ci-release-early 2.1.15 needs Ivy)
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.2")
