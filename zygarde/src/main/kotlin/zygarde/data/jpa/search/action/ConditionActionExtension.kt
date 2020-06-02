package zygarde.data.jpa.search.action

import java.time.LocalDate
import java.time.LocalDateTime
import zygarde.data.jpa.search.request.SearchDateRange
import zygarde.data.jpa.search.request.SearchDateTimeRange

infix fun <T, U> ComparableConditionAction<T, U, LocalDate>.dateRange(dateRange: SearchDateRange?) {
  dateRange?.from?.let { gte(it) }
  dateRange?.to?.let { lte(it) }
}

infix fun <T, U> ComparableConditionAction<T, U, LocalDateTime>.dateTimeRange(dateTimeRange: SearchDateTimeRange?) {
  dateTimeRange?.from?.let { gte(it) }
  dateTimeRange?.until?.let { lt(it) }
}
