package ci.release.early

import org.scalatest._
import scala.util.Try

class UtilsTest extends WordSpec with Matchers {

  "find highest version" in {
    Try { Utils.findHighestVersion(Nil) } shouldBe 'failure
    Utils.findHighestVersion(List("refs/tags/v1.0.0")) shouldBe "1.0.0"
    Utils.findHighestVersion(List("refs/tags/v1.10", "refs/tags/v1.9")) shouldBe "1.10"
    Utils.findHighestVersion(List("refs/tags/validationAttempt5", "refs/tags/v0.1")) shouldBe "0.1"
  }

  "increment version" in {
    Utils.incrementVersion("1") shouldBe "2"
    Utils.incrementVersion("1.0") shouldBe "1.1"
    Utils.incrementVersion("1.0.0") shouldBe "1.0.1"
    Utils.incrementVersion("1.0.0-hotfix") shouldBe "1.0.1"
    Utils.incrementVersion("1.0.0-hotfix2") shouldBe "1.0.1"
  }

}
