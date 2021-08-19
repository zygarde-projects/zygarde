package zygarde.data.jpa.search.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @author leo
 */
@ApiModel
data class PagingRequest(
  @ApiModelProperty(notes = "頁次（從1開始）", required = true)
  var page: Int = 1,
  @ApiModelProperty(notes = "每頁數量", required = true)
  var pageSize: Int = 10
)
