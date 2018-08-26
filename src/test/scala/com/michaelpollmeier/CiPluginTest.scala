package com.michaelpollmeier

import org.scalatest._
import scala.util.Try

class CiReleasePluginTest extends WordSpec with Matchers {

  "find highest version" in {
    Try { CiReleasePlugin.findHighestVersion(Nil) } shouldBe 'failure
    CiReleasePlugin.findHighestVersion(List("refs/tags/v1.0.0")) shouldBe "1.0.0"
    CiReleasePlugin.findHighestVersion(List("refs/tags/v1.10", "refs/tags/v1.9")) shouldBe "1.10"
    CiReleasePlugin.findHighestVersion(List("refs/tags/validationAttempt5", "refs/tags/v0.1")) shouldBe "0.1"
  }

  "increment version" in {
    CiReleasePlugin.incrementVersion("1") shouldBe "2"
    CiReleasePlugin.incrementVersion("1.0") shouldBe "1.1"
    CiReleasePlugin.incrementVersion("1.0.0") shouldBe "1.0.1"
    CiReleasePlugin.incrementVersion("1.0.0-hotfix") shouldBe "1.0.1"
    CiReleasePlugin.incrementVersion("1.0.0-hotfix2") shouldBe "1.0.1"
  }

}
