package ci.release.early

import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UtilsTest extends AnyWordSpec with Matchers {

  "find highest version from taglist" in {
    Utils.findHighestVersion(Nil, println) shouldBe None
    Utils.findHighestVersion(List("a", "b"), println) shouldBe None
    Utils.findHighestVersion(List("v1.0.0"), println) shouldBe Some("1.0.0")
    Utils.findHighestVersion(List("v1.10", "v1.9"), println) shouldBe Some("1.10")
    // TODO fixup separately
    // Utils.findHighestVersion(List("validationAttempt5", "v0.1"), println) shouldBe Some("0.1")
  }

  "increment version" in {
    Utils.incrementVersion("1") shouldBe "2"
    Utils.incrementVersion("1.0") shouldBe "1.1"
    Utils.incrementVersion("1.0.0") shouldBe "1.0.1"
    Utils.incrementVersion("1.0.0-hotfix") shouldBe "1.0.1"
    Utils.incrementVersion("1.0.0-hotfix2") shouldBe "1.0.1"
  }

  "interweave github token into repository url" in {
    val token = "abc"
    Utils.interweaveGithubToken(token, repoUri = "not-a-uri") shouldBe 'failure
    Utils.interweaveGithubToken(token, repoUri = "git@github.com:not-an-http-uri.git") shouldBe 'failure

    Utils.interweaveGithubToken(token, repoUri = "https://github.com/user/repo.git")
      .get shouldBe s"https://${token}@github.com/user/repo.git"
  }

}
