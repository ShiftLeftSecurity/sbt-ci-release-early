package ci.release.early

import com.jsuereth.sbtpgp.SbtPgp
import sbtdynver.DynVerPlugin
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import sbt._
import sbt.plugins.JvmPlugin
import sbt.Keys._
import scala.sys.process._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport.sonatypePublishToBundle

object Plugin extends AutoPlugin {
  object autoImport {
    val verifyNoSnapshotDependencies = taskKey[Unit]("Verify there are no snapshot dependencies (fail otherwise)")
    val ciReleaseSkip = settingKey[Boolean]("skip the release - set to `true` by `ciReleaseSkipIfAlreadyReleased` if the current HEAD is already released, and read by the other steps")
  }
  import autoImport._

  override def requires =
    JvmPlugin && SbtPgp && DynVerPlugin && Sonatype

  override def globalSettings: Seq[Def.Setting[_]] = List(
    Test/publishArtifact := false,
    publishMavenStyle := true,
    /* I tried to define these commands as tasks, but had the following errors:
     * - didn't know how to update the version within the task
     * - didn't figure out how to automatically cross-build without lot's of extra code
    */
    commands += Command.command("ciReleaseSkipIfAlreadyReleased") { state =>
      def log(msg: String) = sLog.value.info(msg)
      Utils.getHeadCommitVersion(log) match {
        case Some(versionTag) => 
          log(s"HEAD is already tagged with a version tag ($versionTag) - setting `ciReleaseSkip := true` so that all other steps will be skipped")
          "set ciReleaseSkip := true" :: state
        case None =>
          log("HEAD does not yet have a version tag - all good")
          state
      }
    },
    commands += Command.command("ciReleaseTagNextVersion") { state =>
      def log(msg: String) = sLog.value.info(msg)
      if (shouldSkip(state)) {
        log("ciReleaseTagNextVersion: skipped")
        state
      } else {
        val tag = Utils.determineAndTagTargetVersion(log).tag
        Utils.push(tag, log)
        log(s"created and pushed $tag")
        log(s"reloading sbt so that sbt-dynver will set the `version` setting based on the git tag ($tag)")
        "verifyNoSnapshotDependencies" :: "reload" :: state
      }
    },
    commands += Command.command("ciRelease") { state =>
      def log(msg: String) = sLog.value.info(msg)
      if (shouldSkip(state)) {
        log("ciRelease: skipped")
        state
      } else {
        log("Running ciRelease")
        "verifyNoSnapshotDependencies" :: "+publish" :: state
      }
    },
    commands += Command.command("ciReleaseSonatype") { state =>
      def log(msg: String) = sLog.value.info(msg)
      if (shouldSkip(state)) {
        log("ciReleaseSonatype: skipped")
        state
      } else {
        log("Running ciReleaseSonatype")
        "verifyNoSnapshotDependencies" ::
          "clean" ::
          "sonatypeBundleClean" ::
          "+publishSigned" ::
          "sonatypeBundleRelease" ::
          "ciReleasePushTag" ::
          state
      }
    },
  )

  def isAzure: Boolean =
    System.getenv("TF_BUILD") == "True"
  def isGithub: Boolean =
    System.getenv("GITHUB_ACTION") != null
  def isCircleCi: Boolean =
    System.getenv("CIRCLECI") == "true"
  def isGitlab: Boolean =
    System.getenv("GITLAB_CI") == "true"

  /** lookup the value of the `ciReleaseSkip` setting */
  def shouldSkip(state: State): Boolean = {
    val extracted = Project.extract(state)
    (extracted.currentRef / ciReleaseSkip).get(extracted.structure.data).getOrElse(false)
  }

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    verifyNoSnapshotDependencies := verifyNoSnapshotDependenciesTask.value,
    ciReleaseSkip := false,
  )

  lazy val verifyNoSnapshotDependenciesTask = Def.task {
    val moduleIds = (Runtime/managedClasspath).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || m.revision.endsWith("-SNAPSHOT")).toList
    assert(snapshots.size == 0, s"expected 0 snapshot dependencies, but found: $snapshots")
  }
}
