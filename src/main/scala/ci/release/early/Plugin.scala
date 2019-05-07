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
    val verifyNoSnapshotDependencies = taskKey[Unit]("Verify there are no snapshot dependencies (fail otherwise)")
  }
  import autoImport._
  import Utils._

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    /* I tried to define these commands as tasks, but had the following errors:
     * - didn't know how to update the version within the task
     * - didn't figure out how to automatically cross-build without lot's of extra code
    */
    commands += Command.command("ci-release") { state =>
      println("Running ci-release")
      verifyGitIsClean 
      val targetVersion = determineTargetVersion
      val tagName = s"v$targetVersion"
      tag(tagName)
      def pushAndReturnState = {
        push(tagName)
        state
      }
      s"""set ThisBuild/version := "$targetVersion"""" ::
        "verifyNoSnapshotDependencies" ::
        "+publish" ::
        pushAndReturnState
    },
    commands += Command.command("ci-release-sonatype") { state =>
      println("Running ci-release-sonatype")
      verifyGitIsClean 
      assert(pgpPassphrase.value.isDefined,
        "please specify PGP_PASSPHRASE as an environment variable (e.g. `export PGP_PASSPHRASE='secret')")
      val targetVersion = determineTargetVersion
      val tagName = s"v$targetVersion"
      tag(tagName)
      def pushAndReturnState = {
        push(tagName)
        state
      }
      s"""set ThisBuild/version := "$targetVersion"""" ::
        "verifyNoSnapshotDependencies" ::
        "sonatypeOpen \"unimportant\"" ::
        "+publishSigned" ::
        "sonatypeRelease" ::
        pushAndReturnState
    }
  )

  override def requires = SbtPgp && Sonatype
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    verifyNoSnapshotDependencies := verifyNoSnapshotDependenciesTask.value
  )

  override def buildSettings: Seq[Def.Setting[_]] = List(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
  )

  lazy val verifyNoSnapshotDependenciesTask = Def.task {
    val moduleIds = (managedClasspath in Runtime).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || m.revision.endsWith("-SNAPSHOT")).toList
    assert(snapshots.size == 0, s"expected 0 snapshot dependencies, but found: $snapshots")
  }
}
