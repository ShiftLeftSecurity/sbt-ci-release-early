package com.michaelpollmeier

import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtPgp.autoImport._
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import sbt.Def
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import sys.process._
import versionsort.VersionHelper
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._

object CiReleasePlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin && SbtPgp && Sonatype

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    commands += Command.command("ci-release") { currentState =>
      println("Running ci-release")
      val allTags = git.tagList.call.asScala.map(_.getName).toList
      val highestVersion = findHighestVersion(allTags)
      println(s"highest version so far: $highestVersion")
      val targetVersion = incrementVersion(highestVersion)
      tagAndPush(s"v$targetVersion")

      s"""set version := "$targetVersion"""" ::
        "publishLocal" ::
        currentState
      // TODO s"""set version := "$targetVersion"""" :: "+publishSigned" :: "sonatypeReleaseAll" :: currentState
    }
  )

  /** based on git tags, derive the highest version */
  private[michaelpollmeier] def findHighestVersion(tags: List[String]): String = {
    val taggedVersions = tags.collect {
      case gitTagVersionRegex(version) => version
    }
    assert(taggedVersions.nonEmpty, "no tagged versions found in git!")
    taggedVersions.sortWith { VersionHelper.compare(_, _) > 0 }.head
  }

  /* TODO allow to configure which part of the version should be incremented, e.g. via sbt.Task */
  private def incrementVersion(version: String): String = {
    val segments = version.split('.')
    val lastSegment = segments.last.takeWhile(_.isDigit).toInt
    val incremented = (lastSegment + 1).toString
    (segments.dropRight(1) :+ incremented).mkString(".")
  }

  private def tagAndPush(tagName: String): Unit = {
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

  lazy val git: Git =
    new Git(new FileRepositoryBuilder().findGitDir(new File(".")).build)

  lazy val gitTagVersionRegex = """refs/tags/v([0-9\.]+)""".r

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := sonatypePublishTo.value
  )
}
