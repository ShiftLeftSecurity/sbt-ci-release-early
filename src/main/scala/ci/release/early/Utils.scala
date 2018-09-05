package ci.release.early

import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import scala.collection.JavaConverters._
import sys.process._
import versionsort.VersionHelper

object Utils {

  def determineAndTagTargetVersion: String = {
    val allTags = git.tagList.call.asScala.map(_.getName).toList
    val highestVersion = findHighestVersion(allTags)
    println(s"highest version so far: $highestVersion")
    val targetVersion = incrementVersion(highestVersion)
    tagAndPush(s"v$targetVersion")
    targetVersion
  }

  /** based on git tags, derive the highest version */
  def findHighestVersion(tags: List[String]): String = {
    val taggedVersions = tags.collect {
      case gitTagVersionRegex(version) => version
    }
    assert(taggedVersions.nonEmpty, "no tagged versions found in git!")
    taggedVersions.sortWith { VersionHelper.compare(_, _) > 0 }.head
  }

  /* TODO allow to configure which part of the version should be incremented, e.g. via sbt.Task */
  def incrementVersion(version: String): String = {
    val segments = version.split('.')
    val lastSegment = segments.last.takeWhile(_.isDigit).toInt
    val incremented = (lastSegment + 1).toString
    (segments.dropRight(1) :+ incremented).mkString(".")
  }

  def tagAndPush(tagName: String): Unit = {
    println(s"tagging as $tagName")
    git.tag.setName(tagName).call

    /* couldn't get jgit to push it to the remote... `There are not any available sig algorithm`
      * TODO use jgit for push */
    val remote: String = {
      val remotes = git.remoteList.call
      assert(remotes.size == 1, "we currently only support repos that have _one_ remote configured, sorry")
      remotes.get(0).getName
    }
    val cmd = s"git push $remote $tagName"
    println(s"pushing to remote, using `$cmd`")
    assert(cmd.! == 0, s"execution failed. command used: `$cmd`")
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
