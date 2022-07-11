package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.collection.ArrayExtensions.takeUntilFirstOccur

class ArrayExtensionsTest {

  @Test
  fun `Array takeUntilFirstOccur`() {
    arrayOf(1, 2, 3).takeUntilFirstOccur { it == 1 }.size shouldBe 1
    arrayOf(1, 2, 3).takeUntilFirstOccur { it == 2 }.size shouldBe 2
    arrayOf(1, 2, 3).takeUntilFirstOccur(false) { it == 2 }.size shouldBe 1
    arrayOf(1, 2, 3).takeUntilFirstOccur { it == 8 }.size shouldBe 3
  }
}
