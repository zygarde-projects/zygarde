package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * @author leo
 */
@Schema
open class SearchDateTimeRange(
  @Schema(description = "開始時間")
  var from: LocalDateTime? = null,
  @Schema(description = "結束時間（不包含）")
  var until: LocalDateTime? = null
)
