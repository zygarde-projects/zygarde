package zygarde.core.extension.general

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * @author leo
 */
class NullHandlingExtensionsTest : StringSpec(
  {
    "should be able to fallback" {
      val map = mapOf("foo" to "bar", "bar" to null)
      map["foo"].fallbackWhenNull { "gg" } shouldBe "bar"
      map["bar"].fallbackWhenNull { "gg" } shouldBe "gg"
      map["bar"].fallbackWhenNull("bb") shouldBe "bb"
    }
  }
)
