package com.geirsson

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
      val highestVersion = findHighestVersion
      println(s"highest version so far: $highestVersion")
      val targetVersion = incrementVersion(highestVersion)

      println(s"setting target version to $targetVersion (pushing tag)")
      git.tag.setName(s"v$targetVersion").call
      // git.push.
      // TODO git push

      // TODO "+publishSigned" :: "sonatypeReleaseAll" :: currentState
      currentState
    }
  )

  /** based on git tags, derive the highest version */
  private def findHighestVersion: String = {
    val allTags = git.tagList.call.asScala
    val taggedVersions = allTags.map(_.getName).collect {
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
    (segments.dropRight(1) :+ incremented).mkString
  }

  lazy val git: Git =
    new Git(new FileRepositoryBuilder().findGitDir(new File(".")).build)

  lazy val gitTagVersionRegex = """refs/tags/v(.*?)""".r

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := sonatypePublishTo.value
  )
}
