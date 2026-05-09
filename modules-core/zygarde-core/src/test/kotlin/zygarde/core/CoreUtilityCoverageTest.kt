package zygarde.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import zygarde.core.utils.DateUtils
import zygarde.ctx.ApiVersionContext
import zygarde.data.api.KeywordPagingAndSortingRequest
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.PagingRequest
import zygarde.data.api.SortField
import zygarde.data.search.range.SearchDateRangeOverlap
import zygarde.data.search.range.SearchDateTimeRangeOverlap
import zygarde.data.search.range.SearchIntRangeOverlap
import zygarde.data.search.range.SearchLongRangeOverlap
import zygarde.data.search.range.SearchRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class CoreUtilityCoverageTest {
  @AfterEach
  fun tearDown() {
    ApiVersionContext.clear()
  }

  @Test
  fun `ApiVersionContext should default set and clear versions`() {
    ApiVersionContext.version() shouldBe Long.MAX_VALUE
    ApiVersionContext.setVersion(3)
    ApiVersionContext.version() shouldBe 3
    ApiVersionContext.clear()
    ApiVersionContext.version() shouldBe Long.MAX_VALUE
  }

  @Test
  fun `DateUtils overlap should detect all overlap positions and disjoint ranges`() {
    val start = LocalDateTime.of(2026, 5, 9, 10, 0)
    val end = LocalDateTime.of(2026, 5, 9, 12, 0)

    DateUtils.overlap(start, end, start.minusHours(1), start.plusMinutes(1)) shouldBe true
    DateUtils.overlap(start, end, end.minusMinutes(1), end.plusHours(1)) shouldBe true
    DateUtils.overlap(start, end, start.plusMinutes(30), end.minusMinutes(30)) shouldBe true
    DateUtils.overlap(start, end, start.minusHours(2), end.plusHours(2)) shouldBe true
    DateUtils.overlap(start, end, end.plusMinutes(1), end.plusHours(1)) shouldBe false
  }

  @Test
  fun `search range DTOs should store configured values`() {
    SearchRange.Number.SearchRangeDouble(1.5, 2.5).also {
      it.from shouldBe 1.5
      it.to shouldBe 2.5
    }
    SearchRange.Number.SearchRangeInt(1, 2).from shouldBe 1
    SearchRange.Number.SearchRangeLong(1L, 2L).to shouldBe 2L
    SearchRange.Number.SearchRangeBigDecimal(BigDecimal.ONE, BigDecimal.TEN).to shouldBe BigDecimal.TEN
    SearchRange.Date.SearchRangeLocalDate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)).from shouldBe LocalDate.of(2026, 1, 1)
    SearchRange.Date.SearchRangeLocalDateTime(
      LocalDateTime.of(2026, 1, 1, 0, 0),
      LocalDateTime.of(2026, 1, 2, 0, 0)
    ).to shouldBe LocalDateTime.of(2026, 1, 2, 0, 0)
    SearchIntRangeOverlap(1, 2).end shouldBe 2
    SearchLongRangeOverlap(1, 2).start shouldBe 1
    SearchDateRangeOverlap(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)).end shouldBe LocalDate.of(2026, 1, 2)
    SearchDateTimeRangeOverlap(
      LocalDateTime.of(2026, 1, 1, 0, 0),
      LocalDateTime.of(2026, 1, 2, 0, 0)
    ).start shouldBe LocalDateTime.of(2026, 1, 1, 0, 0)
  }

  @Test
  fun `paging request DTOs should expose mutable paging sorting and keyword fields`() {
    val pagingAndSortingRequest = PagingAndSortingRequest().also {
      it.paging = PagingRequest(page = 2, pageSize = 20)
      it.sorts = listOf(SortField(field = "name"))
    }
    val keywordRequest = KeywordPagingAndSortingRequest().also {
      it.keyword = "zygarde"
    }

    pagingAndSortingRequest.paging.page shouldBe 2
    pagingAndSortingRequest.sorts?.first()?.field shouldBe "name"
    keywordRequest.keyword shouldBe "zygarde"
  }
}
