package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * @author leo
 */
@Schema
open class SearchDateRange(
  @Schema(description = "開始日期")
  var from: LocalDate? = null,
  @Schema(description = "結束日期（包含）")
  var to: LocalDate? = null
)
