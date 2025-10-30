package zygarde.data.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SortDirectionTest {

  @Test
  fun `should have ASC and DESC values`() {
    // given & when
    val values = SortDirection.values()

    // then
    values.size shouldBe 2
    values[0] shouldBe SortDirection.ASC
    values[1] shouldBe SortDirection.DESC
  }

  @Test
  fun `should parse from string`() {
    // given & when & then
    SortDirection.valueOf("ASC") shouldBe SortDirection.ASC
    SortDirection.valueOf("DESC") shouldBe SortDirection.DESC
  }
}
