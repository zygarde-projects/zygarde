package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @author leo
 */
@Schema
data class PagingRequest(
  @Schema(description = "頁次（從1開始）", required = true)
  var page: Int = 1,
  @Schema(description = "每頁數量", required = true)
  var pageSize: Int = 10
)
