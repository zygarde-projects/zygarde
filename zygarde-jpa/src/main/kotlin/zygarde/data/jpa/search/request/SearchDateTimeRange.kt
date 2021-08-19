package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

/**
 * @author leo
 */
@ApiModel
open class SearchDateTimeRange(
  @ApiModelProperty(notes = "開始時間")
  var from: LocalDateTime? = null,
  @ApiModelProperty(notes = "結束時間（不包含）")
  var until: LocalDateTime? = null
)
