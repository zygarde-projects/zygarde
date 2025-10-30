package zygarde.core.di.autoconfigure

import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import zygarde.core.di.DiServiceContext

class ZygardeDiConfigTest {

  @Test
  fun `should create DiServiceContext bean`() {
    // given
    val config = ZygardeDiConfig()

    // when
    val bean = config.diServiceContext()

    // then
    bean shouldBeSameInstanceAs DiServiceContext
  }
}
