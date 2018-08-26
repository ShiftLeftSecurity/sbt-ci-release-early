package ci.release.early

import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._

object Plugin extends AutoPlugin {
  import Utils._

  override def trigger = allRequirements
  override def requires = SbtPgp && Sonatype

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    commands += Command.command("ci-release") { currentState =>
      println("Running ci-release")
      verifyGitIsClean 
      val targetVersion = determineAndTagTargetVersion
      s"""set version := "$targetVersion"""" ::
        "+publish" ::
        currentState
    },
    commands += Command.command("ci-release-sonatype") { currentState =>
      println("Running ci-release-sonatype")
      verifyGitIsClean 
      assert(pgpPassphrase.value.isDefined,
        "please specify PGP_PASSPHRASE as an evironment variable (e.g. `export PGP_PASSPHRASE='secret')")
      val targetVersion = determineAndTagTargetVersion
      s"""set version := "$targetVersion"""" ::
        "+publishSigned" ::
        "sonatypeReleaseAll" ::
        currentState
    }
  )

  override def buildSettings: Seq[Def.Setting[_]] = List(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
  )

}
