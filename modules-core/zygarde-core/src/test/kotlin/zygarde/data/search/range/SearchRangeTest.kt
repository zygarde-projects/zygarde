package zygarde.data.search.range

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SearchRangeTest {

  @Test
  fun `should create SearchRangeInt with values`() {
    // given & when
    val range = SearchRange.Number.SearchRangeInt(from = 1, to = 10)

    // then
    range.from shouldBe 1
    range.to shouldBe 10
    range.fromExclusive shouldBe null
    range.toExclusive shouldBe null
  }

  @Test
  fun `should create SearchRangeLong with values`() {
    // given & when
    val range = SearchRange.Number.SearchRangeLong(from = 100L, to = 1000L)

    // then
    range.from shouldBe 100L
    range.to shouldBe 1000L
  }

  @Test
  fun `should create SearchRangeDouble with values`() {
    // given & when
    val range = SearchRange.Number.SearchRangeDouble(from = 1.5, to = 10.5)

    // then
    range.from shouldBe 1.5
    range.to shouldBe 10.5
  }

  @Test
  fun `should create SearchRangeBigDecimal with values`() {
    // given
    val from = BigDecimal("100.50")
    val to = BigDecimal("999.99")

    // when
    val range = SearchRange.Number.SearchRangeBigDecimal(from = from, to = to)

    // then
    range.from shouldBe from
    range.to shouldBe to
  }

  @Test
  fun `should create SearchRangeLocalDate with values`() {
    // given
    val from = LocalDate.of(2023, 1, 1)
    val to = LocalDate.of(2023, 12, 31)

    // when
    val range = SearchRange.Date.SearchRangeLocalDate(from = from, to = to)

    // then
    range.from shouldBe from
    range.to shouldBe to
  }

  @Test
  fun `should create SearchRangeLocalDateTime with values`() {
    // given
    val from = LocalDateTime.of(2023, 1, 1, 0, 0)
    val to = LocalDateTime.of(2023, 12, 31, 23, 59)

    // when
    val range = SearchRange.Date.SearchRangeLocalDateTime(from = from, to = to)

    // then
    range.from shouldBe from
    range.to shouldBe to
  }

  @Test
  fun `should support exclusive bounds`() {
    // given & when
    val range = SearchRange.Number.SearchRangeInt(from = 1, to = 10)
    range.fromExclusive = true
    range.toExclusive = true

    // then
    range.fromExclusive shouldBe true
    range.toExclusive shouldBe true
  }

  @Test
  fun `should allow null values for open-ended ranges`() {
    // given & when
    val range1 = SearchRange.Number.SearchRangeInt(from = null, to = 10)
    val range2 = SearchRange.Number.SearchRangeInt(from = 1, to = null)

    // then
    range1.from shouldBe null
    range1.to shouldBe 10
    range2.from shouldBe 1
    range2.to shouldBe null
  }
}
