package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.collection.CollectionExtensions.takeUntilFirstOccur

class CollectionExtensionsTest {

  @Test
  fun `Collection takeUntilFirstOccur`() {
    listOf(1, 2, 3).takeUntilFirstOccur { it == 1 }.size shouldBe 1
    listOf(1, 2, 3).takeUntilFirstOccur { it == 2 }.size shouldBe 2
    listOf(1, 2, 3).takeUntilFirstOccur(false) { it == 2 }.size shouldBe 1
    listOf(1, 2, 3).takeUntilFirstOccur { it == 8 }.size shouldBe 3
  }
}
