package com.geirsson

import com.typesafe.sbt.GitPlugin
import sbtdynver.DynVerPlugin.autoImport._
import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt.Def
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtdynver.DynVerPlugin
import sys.process._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.autoImport._

object CiReleasePlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires =
    JvmPlugin && SbtPgp && DynVerPlugin && GitPlugin && Sonatype

  override def buildSettings: Seq[Def.Setting[_]] = List(
    dynverSonatypeSnapshots := true
  )

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    commands += Command.command("ci-release") { currentState =>
      println("Running ci-release")
      "+publishSigned" :: "sonatypeRelease" :: currentState
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] = List(
    publishTo := sonatypePublishTo.value
  )

}
