package zygarde.data.search.range

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

open class SearchRange<T : Comparable<T>> {
  @Schema(description = "from value, included by default")
  open var from: T? = null

  @Schema(description = "to value, included by default")
  open var to: T? = null

  @Schema(description = "exclude 'from' value, default false")
  open var fromExclusive: Boolean? = null

  @Schema(description = "exclude 'to' value, default false")
  open var toExclusive: Boolean? = null

  class Number {
    class SearchRangeDouble(override var from: Double?, override var to: Double?) : SearchRange<Double>()
    class SearchRangeInt(override var from: Int?, override var to: Int?) : SearchRange<Int>()
    class SearchRangeLong(override var from: Long?, override var to: Long?) : SearchRange<Long>()
    class SearchRangeBigDecimal(override var from: BigDecimal?, override var to: BigDecimal?) : SearchRange<BigDecimal>()
  }

  class Date {
    class SearchRangeLocalDate(override var from: LocalDate?, override var to: LocalDate?) : SearchRange<LocalDate>()
    class SearchRangeLocalDateTime(override var from: LocalDateTime?, override var to: LocalDateTime?) : SearchRange<LocalDateTime>()
  }
}
