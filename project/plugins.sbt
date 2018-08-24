addSbtPlugin("io.get-coursier" % "sbt-coursier" % coursier.util.Properties.version)
addSbtPlugin("io.get-coursier" % "sbt-shading" % coursier.util.Properties.version)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
