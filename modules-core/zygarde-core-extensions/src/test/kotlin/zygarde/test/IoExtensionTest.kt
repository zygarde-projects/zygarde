package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.extension.io.safeCopyTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class IoExtensionTest {
  @Test
  fun `InputStream safeCopyTo`() {
    val baos = ByteArrayOutputStream()
    ByteArrayInputStream("test".toByteArray()).safeCopyTo(baos)
    String(baos.toByteArray()) shouldBe "test"
  }
}
