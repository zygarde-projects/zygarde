package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.general.fallbackWhenNull

/**
 * @author leo
 */
class NullHandlingExtensionsTest {
  @Test
  fun `should be able to fallback`() {
    val map = mapOf("foo" to "bar", "bar" to null)
    map["foo"].fallbackWhenNull { "gg" } shouldBe "bar"
    map["bar"].fallbackWhenNull { "gg" } shouldBe "gg"
    map["bar"].fallbackWhenNull("bb") shouldBe "bb"
  }
}
