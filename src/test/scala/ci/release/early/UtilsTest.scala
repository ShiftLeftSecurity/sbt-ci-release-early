package ci.release.early

import org.scalatest._

class UtilsTest extends WordSpec with Matchers {

  "find highest version from taglist" in {
    Utils.findHighestVersion(List("refs/tags/v1.0.0"), println) shouldBe "1.0.0"
    Utils.findHighestVersion(List("refs/tags/v1.10", "refs/tags/v1.9"), println) shouldBe "1.10"
    Utils.findHighestVersion(List("refs/tags/validationAttempt5", "refs/tags/v0.1"), println) shouldBe "0.1"
  }

  "defaults to `0.1.0` if no version tags available" in {
    Utils.findHighestVersion(Nil, println) shouldBe "0.1.0"
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
