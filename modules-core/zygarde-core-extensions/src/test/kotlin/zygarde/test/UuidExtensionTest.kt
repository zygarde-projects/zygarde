package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.uuid.to32digits
import java.util.UUID

class UuidExtensionTest {
  @Test
  fun `generate 32 digits uuid`() {
    UUID.randomUUID().to32digits().length shouldBe 32
  }
}
