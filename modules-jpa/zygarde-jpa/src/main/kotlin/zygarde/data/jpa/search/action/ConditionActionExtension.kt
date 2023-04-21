package zygarde.data.jpa.search.action

import zygarde.data.search.SearchDateRange
import zygarde.data.search.SearchDateTimeRange
import zygarde.data.search.range.SearchRange
import java.time.LocalDate
import java.time.LocalDateTime

infix fun <T, U> ComparableConditionAction<T, U, LocalDate>.dateRange(dateRange: SearchDateRange?) = range(
  SearchRange.Date.SearchRangeLocalDate(dateRange?.from, dateRange?.to)
)

infix fun <T, U> ComparableConditionAction<T, U, LocalDateTime>.dateTimeRange(dateTimeRange: SearchDateTimeRange?) = range(
  SearchRange.Date.SearchRangeLocalDateTime(dateTimeRange?.from, dateTimeRange?.until)
    .apply {
      toExclusive = true
    }
)

infix fun <E, F : Comparable<F>> ComparableConditionAction<*, E, F>.range(range: SearchRange<F>?) {
  range?.from?.let {
    if (range.fromExclusive == true) {
      gt(it)
    } else {
      gte(it)
    }
  }
  range?.to?.let {
    if (range.toExclusive == true) {
      lt(it)
    } else {
      lte(it)
    }
  }
}
