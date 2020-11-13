package ci.release.early

import com.jsuereth.sbtpgp.SbtPgp
import com.typesafe.sbt.GitPlugin
import java.util.Base64
import sbt._
import sbt.plugins.JvmPlugin
import sbt.Keys._
import xerial.sbt.Sonatype

import java.nio.file.Files
import java.nio.file.Paths
import scala.sys.process._

object Plugin extends AutoPlugin {
  object autoImport {
    val verifyNoSnapshotDependencies = taskKey[Unit]("Verify there are no snapshot dependencies (fail otherwise)")
  }
  import autoImport._

  override def requires =
    JvmPlugin && SbtPgp && GitPlugin && Sonatype

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    /* I tried to define these commands as tasks, but had the following errors:
     * - didn't know how to update the version within the task
     * - didn't figure out how to automatically cross-build without lot's of extra code
    */
    commands += Command.command("ciReleaseTagNextVersion") { state =>
      def log(msg: String) = sLog.value.info(msg)
      val tag = Utils.determineAndTagTargetVersion(log).tag
      Utils.push(tag, log)
      sLog.value.info("reloading sbt so that sbt-git will set the `version`" +
        s" setting based on the git tag ($tag)")
      "verifyNoSnapshotDependencies" :: "reload" :: state
      },
    commands += Command.command("ciRelease") { state =>
      sLog.value.info("Running ciRelease")
      "verifyNoSnapshotDependencies" :: "+publish" :: state
    },
    commands += Command.command("ciReleaseSonatype") { state =>
      sLog.value.info("Running ciReleaseSonatype")
      setupGpg()
      val reloadKeyFiles =
        "; set pgpSecretRing := pgpSecretRing.value; set pgpPublicRing := pgpPublicRing.value"

      reloadKeyFiles ::
      "verifyNoSnapshotDependencies" ::
        "clean" ::
        "sonatypeBundleClean" ::
        "+publishSigned" ::
        "sonatypeBundleRelease" ::
        state
    },
  )

  def setupGpg(): Unit = {
    List("gpg", "--version").!
    val secret = sys.env("PGP_SECRET")
    if (isAzure) {
      // base64 encoded gpg secrets are too large for Azure variables but
      // they fit within the 4k limit when compressed.
      Files.write(Paths.get("gpg.zip"), Base64.getDecoder.decode(secret))
      s"unzip gpg.zip".!
      s"gpg --import gpg.key".!
    } else {
      (s"echo $secret" #| "base64 --decode" #| "gpg --import").!
    }
  }

  def isAzure: Boolean =
    System.getenv("TF_BUILD") == "True"
  def isGithub: Boolean =
    System.getenv("GITHUB_ACTION") != null
  def isCircleCi: Boolean =
    System.getenv("CIRCLECI") == "true"
  def isGitlab: Boolean =
    System.getenv("GITLAB_CI") == "true"

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    verifyNoSnapshotDependencies := verifyNoSnapshotDependenciesTask.value,
    publishTo := sonatypePublishToBundle.value
  )

  lazy val verifyNoSnapshotDependenciesTask = Def.task {
    val moduleIds = (managedClasspath in Runtime).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || m.revision.endsWith("-SNAPSHOT")).toList
    assert(snapshots.size == 0, s"expected 0 snapshot dependencies, but found: $snapshots")
  }
}
