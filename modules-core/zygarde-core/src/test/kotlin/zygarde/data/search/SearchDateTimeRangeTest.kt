package zygarde.data.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SearchDateTimeRangeTest {

  @Test
  fun `should create SearchDateTimeRange with default values`() {
    // given & when
    val range = SearchDateTimeRange()

    // then
    range.from shouldBe null
    range.until shouldBe null
  }

  @Test
  fun `should create SearchDateTimeRange with datetime`() {
    // given
    val from = LocalDateTime.of(2023, 1, 1, 0, 0)
    val until = LocalDateTime.of(2023, 12, 31, 23, 59)

    // when
    val range = SearchDateTimeRange(
      from = from,
      until = until
    )

    // then
    range.from shouldBe from
    range.until shouldBe until
  }

  @Test
  fun `should allow modifying values`() {
    // given
    val range = SearchDateTimeRange()
    val from = LocalDateTime.of(2024, 6, 1, 10, 0)
    val until = LocalDateTime.of(2024, 6, 30, 18, 0)

    // when
    range.from = from
    range.until = until

    // then
    range.from shouldBe from
    range.until shouldBe until
  }
}
