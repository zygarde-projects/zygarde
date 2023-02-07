package zygarde.data.search.range

import java.time.LocalDate
import java.time.LocalDateTime

abstract class SearchRangeOverlap<T>(
  var start: T,
  var end: T,
)

class SearchIntRangeOverlap(start: Int, end: Int) : SearchRangeOverlap<Int>(start, end)
class SearchLongRangeOverlap(start: Long, end: Long) : SearchRangeOverlap<Long>(start, end)
class SearchDateRangeOverlap(start: LocalDate, end: LocalDate) : SearchRangeOverlap<LocalDate>(start, end)
class SearchDateTimeRangeOverlap(start: LocalDateTime, end: LocalDateTime) : SearchRangeOverlap<LocalDateTime>(start, end)
