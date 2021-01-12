package ci.release.early

import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import scala.collection.JavaConverters._
import scala.util.Try
import sys.process._
import versionsort.VersionHelper

case class VersionAndTag(version: String, tag: String)

object Version {

  val majorMinorPatch = "(\\d+)\\.(\\d+).(\\d+)-?.*".r
  val majorMinor      = "(\\d+)\\.(\\d+)".r
  val major           = "(\\d+)".r

  def unapply(string: String): Option[(Option[Int], Option[Int], Option[Int])] = string match {
    case majorMinorPatch(major, minor, patch) => Some(Some(major.toInt), Some(minor.toInt), Some(patch.toInt))
    case majorMinor(major, minor) => Some(Some(major.toInt), Some(minor.toInt), None)
    case major(major)             => Some(Some(major.toInt), None, None)
    case _                        => None
  }

}

trait Increment
object Increment {
  case object Major extends Increment
  case object Minor extends Increment
  case object Patch extends Increment
}

object Utils {

  def determineAndTagTargetVersion(log: String => Any, increment: Increment): VersionAndTag = {
    verifyGitIsClean
    val targetVersion = Utils.determineTargetVersion(log, increment)
    val tagName = s"v$targetVersion"
    tag(tagName, log)
    VersionAndTag(targetVersion, tagName)
  }

  def determineTargetVersion(log: String => Any, increment: Increment): String = {
    val allTags = git.tagList.call.asScala.map(_.getName).toList
    val highestVersion = findHighestVersion(allTags, log)
    log(s"highest version so far: $highestVersion")
    val targetVersion = incrementVersion(highestVersion, increment)
    targetVersion
  }

  /** based on git tags, derive the highest version */
  def findHighestVersion(tags: List[String], log: String => Any): String = {
    val taggedVersions = tags.collect {
      case gitTagVersionRegex(version) => version
    }

    if (taggedVersions.isEmpty) {
      val defaultVersion = "0.1.0"
      log(s"no tagged versions found in git, starting with $defaultVersion")
      defaultVersion
    } else {
      taggedVersions.sortWith { VersionHelper.compare(_, _) > 0 }.head
    }
  }

  def incrementVersion(version: String, increment: Increment): String = (version, increment) match {
    case (Version(Some(major), Some(minor), Some(patch)), Increment.Major) => s"${major + 1}.0.0"
    case (Version(Some(major), Some(minor), Some(patch)), Increment.Minor) => s"$major.${minor + 1}.0"
    case (Version(Some(major), Some(minor), Some(patch)), Increment.Patch) => s"$major.$minor.${patch + 1}"
    case (Version(Some(major), Some(minor), None), Increment.Major)        => s"${major + 1}.0"
    case (Version(Some(major), Some(minor), None), Increment.Minor)        => s"$major.${minor + 1}"
    case (Version(Some(major), Some(minor), None), Increment.Patch)        => s"$major.$minor.1"
    case (Version(Some(major), None, None), Increment.Major)               => s"${major + 1}"
    case (Version(Some(major), None, None), Increment.Minor)               => s"$major.1"
    case (Version(Some(major), None, None), Increment.Patch)               => s"$major.0.1"
    case (version, _)                                                      => sys.error(s"Unable to extract version from $version")
  }

  def tag(tagName: String, log: String => Any): Unit = {
    log(s"tagging as $tagName")
    git.tag.setName(tagName).call
  }

  def push(tagName: String, log: String => Any): Unit = {
    /* couldn't get jgit to push it to the remote... falling back to installed version of git
     * jgit error: `There are not any available sig algorithm`... no idea */
    val remoteUri: String = {
      val remotes = git.remoteList.call
      assert(remotes.size == 1, "we currently only support repos that have _one_ remote configured, sorry")
      val uri = remotes.get(0).getURIs.get(0).toString
      log(s"pushing $tagName to $uri")
      Option(System.getenv("GITHUB_TOKEN")) match {
        case None => uri
        case Some(token) =>
          log(s"env var GITHUB_TOKEN found, trying to interweave it with the remote uri ($uri)")
          interweaveGithubToken(token, uri).get
      }
    }

    val cmd = s"git push $remoteUri $tagName"
    assert(cmd.! == 0, s"failed to push the new tag to the remote ($remoteUri)")
  }

  /** note: only works for https urls */
  def interweaveGithubToken(token: String, repoUri: String): Try[String] =
    Try {
      assert(repoUri.startsWith("https://"), "interlacing github tokens only works with `https://` urls")
      s"https://${token}@" + repoUri.drop(8)
    }

  def verifyGitIsClean = {
    val status = git.status.call
    val path = git.getRepository.getDirectory.getAbsolutePath
    assert(status.isClean, 
      s"""git repository ($path) isn't clean. Some indication of what it may be:
         |changed: ${status.getChanged}
         |changed: ${status.getChanged}
         |conflicting: ${status.getConflicting}
         |added: ${status.getAdded}
         |missing: ${status.getMissing}
         |modified: ${status.getModified}
         |removed: ${status.getRemoved}
         |uncommitted: ${status.getUncommittedChanges}
         |untracked: ${status.getUntracked}
         |untrackedFolders: ${status.getUntrackedFolders}
      """.stripMargin)
  }

  lazy val git: Git =
    new Git(new FileRepositoryBuilder().findGitDir(new File(".")).build)

  lazy val gitTagVersionRegex = """refs/tags/v([0-9\.]+)""".r
}
