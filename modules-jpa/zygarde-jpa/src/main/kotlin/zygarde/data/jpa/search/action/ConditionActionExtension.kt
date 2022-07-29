package zygarde.data.jpa.search.action

import zygarde.data.search.SearchDateRange
import zygarde.data.search.SearchDateTimeRange
import zygarde.data.search.SearchDoubleRange
import zygarde.data.search.SearchIntRange
import java.time.LocalDate
import java.time.LocalDateTime

infix fun <T, U> ComparableConditionAction<T, U, LocalDate>.dateRange(dateRange: SearchDateRange?) {
  dateRange?.from?.let { gte(it) }
  dateRange?.to?.let { lte(it) }
}

infix fun <T, U> ComparableConditionAction<T, U, LocalDateTime>.dateTimeRange(dateTimeRange: SearchDateTimeRange?) {
  dateTimeRange?.from?.let { gte(it) }
  dateTimeRange?.until?.let { lt(it) }
}

infix fun <T, U> ComparableConditionAction<T, U, Double>.numRange(numberRange: SearchDoubleRange?) {
  numberRange?.from?.let { gte(it) }
  numberRange?.to?.let { lt(it) }
}

infix fun <T, U> ComparableConditionAction<T, U, Int>.numRange(numberRange: SearchIntRange?) {
  numberRange?.from?.let { gte(it) }
  numberRange?.to?.let { lt(it) }
}
