package ci.release.early

import java.io.File
import sbt._
import sbt.Keys._
import ci.release.early.Utils._

object Plugin extends AutoPlugin {

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = List(
    publishArtifact.in(Test) := false,
    publishMavenStyle := true,
    commands += Command.command("ci-release") { currentState =>
      println("Running ci-release")
      val targetVersion = determineAndTagTargetVersion
      s"""set version := "$targetVersion"""" ::
        "+publish" ::
        currentState
    }
  )

}
