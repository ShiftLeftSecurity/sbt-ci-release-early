package ci.release.early

import sbt._
import sbt.Keys._

object Plugin extends AutoPlugin {
  object autoImport {
    val verifyNoSnapshotDependencies = taskKey[Unit]("Verify there are no snapshot dependencies (fail otherwise)")
  }
  import autoImport._

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
  )

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    verifyNoSnapshotDependencies := verifyNoSnapshotDependenciesTask.value
  )

  lazy val verifyNoSnapshotDependenciesTask = Def.task {
    val moduleIds = (managedClasspath in Runtime).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || m.revision.endsWith("-SNAPSHOT")).toList
    assert(snapshots.size == 0, s"expected 0 snapshot dependencies, but found: $snapshots")
  }
}
