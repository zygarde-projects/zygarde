package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

/**
 * @author leo
 */
@ApiModel
open class SearchDateRange(
  @ApiModelProperty(notes = "開始日期")
  var from: LocalDate? = null,
  @ApiModelProperty(notes = "結束日期（包含）")
  var to: LocalDate? = null
)
