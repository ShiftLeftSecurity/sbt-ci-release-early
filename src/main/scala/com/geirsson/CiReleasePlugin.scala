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
      println(s"highest version: $highestVersion")
      // println("auto-incrementing version to: TODO")
      // println("TODO git tag && push")
      // TODO "+publishSigned" :: "sonatypeRelease" :: currentState
      currentState
    }
  )

    /* TODO allow to configure which part of the version should be incremented, e.g. via sbt.Task */
  // private def deriveNextVersion: String = {

  /** based on git tags, derive the highest version */
  private def findHighestVersion: String = {
    val allTags = git.tagList.call.asScala
    val taggedVersions = allTags.map(_.getName).collect {
      case gitTagVersionRegex(version) => version
    }
    assert(taggedVersions.nonEmpty, "no tagged versions found in git!")
    val sorted = taggedVersions.sortWith { (a, b) => VersionHelper.compare(a, b) > 0 }
    sorted.head
  }

  lazy val git: Git =
    new Git(new FileRepositoryBuilder().findGitDir(new File(".")).build)

  lazy val gitTagVersionRegex = """refs/tags/v(.*?)""".r

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := sonatypePublishTo.value
  )
}
