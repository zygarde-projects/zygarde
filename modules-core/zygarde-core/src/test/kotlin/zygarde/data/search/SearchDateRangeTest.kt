package zygarde.data.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SearchDateRangeTest {
  @Test
  fun `should create SearchDateRange with default values`() {
    // given & when
    val range = SearchDateRange()

    // then
    range.from shouldBe null
    range.to shouldBe null
  }

  @Test
  fun `should create SearchDateRange with dates`() {
    // given
    val fromDate = LocalDate.of(2023, 1, 1)
    val toDate = LocalDate.of(2023, 12, 31)

    // when
    val range =
      SearchDateRange(
        from = fromDate,
        to = toDate,
      )

    // then
    range.from shouldBe fromDate
    range.to shouldBe toDate
  }

  @Test
  fun `should allow modifying values`() {
    // given
    val range = SearchDateRange()
    val fromDate = LocalDate.of(2024, 1, 1)
    val toDate = LocalDate.of(2024, 6, 30)

    // when
    range.from = fromDate
    range.to = toDate

    // then
    range.from shouldBe fromDate
    range.to shouldBe toDate
  }
}
