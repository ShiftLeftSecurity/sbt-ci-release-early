package ci.release.early

import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._

object SonatypePlugin extends AutoPlugin {
  import Utils._

  override def trigger = allRequirements
  override def requires = SbtPgp && Sonatype

  override def buildSettings: Seq[Def.Setting[_]] = List(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := sonatypePublishTo.value
  )

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    commands += Command.command("ci-release-sonatype") { currentState =>
      assert(pgpPassphrase.value.isDefined,
        "please specify PGP_PASSPHRASE as an evironment variable (e.g. `export PGP_PASSPHRASE='secret')")
      println("Running ci-release-sonatype")
      val targetVersion = determineAndTagTargetVersion
      s"""set version := "$targetVersion"""" ::
        "+publishSigned" ::
        "sonatypeReleaseAll" ::
        currentState
    }
  )

}
