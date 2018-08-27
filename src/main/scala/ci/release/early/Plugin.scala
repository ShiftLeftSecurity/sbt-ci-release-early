package ci.release.early

import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtPgp.autoImport._
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._
import xerial.sbt.Sonatype.SonatypeCommand.sonatypeReleaseAll

object Plugin extends AutoPlugin {
  object autoImport {
    val ciRelease = taskKey[Unit]("determine and tag the next version and release it")
    val ciReleaseSonatype = taskKey[Unit]("determine and tag the next version and release it to sonatype")
    val verifyNoSnapshotDependencies = taskKey[Unit]("Verify there are no snapshot dependencies (fail otherwise)")
  }
  import autoImport._
  import Utils._

  override def trigger = allRequirements
  override def requires = SbtPgp && Sonatype

  override lazy val projectSettings = Seq(
    verifyNoSnapshotDependencies := verifyNoSnapshotDependenciesTask.value,
    ciRelease := ciReleaseTask.value,
    ciReleaseSonatype := ciReleaseSonatypeTask.value
  )

  lazy val verifyNoSnapshotDependenciesTask = Def.task {
    val moduleIds = (managedClasspath in Runtime).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || m.revision.endsWith("-SNAPSHOT")).toList
    assert(snapshots.size == 0, s"expected 0 snapshot dependencies, but found: $snapshots")
  }

  lazy val ciReleaseTask = Def.task {
    println("Running ciRelease")
    verifyGitIsClean 
    verifyNoSnapshotDependenciesTask.value
    val targetVersion = determineAndTagTargetVersion
    publishLocal.value
    publish.value
  }

  lazy val ciReleaseSonatypeTask = Def.task {
    println("Running ciReleaseSonatype")
    verifyGitIsClean 
    verifyNoSnapshotDependenciesTask.value
    assert(pgpPassphrase.value.isDefined,
      "please specify PGP_PASSPHRASE as an evironment variable (e.g. `export PGP_PASSPHRASE='secret')")
    val targetVersion = determineAndTagTargetVersion
    version := targetVersion
    publishSigned.value
    sonatypeReleaseAll
  }

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true
    // commands += Command.command("ci-release") { state =>
    //   println("Running ci-release")
    //   verifyGitIsClean 
    //   val targetVersion = determineAndTagTargetVersion
    //   s"""set version := "$targetVersion"""" ::
    //     "+publish" ::
    //     state
    // },
    // commands += Command.command("ci-release-sonatype") { state =>
    //   println("Running ci-release-sonatype")
    //   verifyGitIsClean 
    //   assert(pgpPassphrase.value.isDefined,
    //     "please specify PGP_PASSPHRASE as an evironment variable (e.g. `export PGP_PASSPHRASE='secret')")
    //   val targetVersion = determineAndTagTargetVersion
    //   s"""set version := "$targetVersion"""" ::
    //     "+publishSigned" ::
    //     "sonatypeReleaseAll" ::
    //     state
    // }
  )

  override def buildSettings: Seq[Def.Setting[_]] = List(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
  )

}
