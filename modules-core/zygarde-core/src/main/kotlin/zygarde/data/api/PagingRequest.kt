package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @author leo
 */
@Schema
data class PagingRequest(
  @Schema(description = "頁次（從1開始）", requiredMode = Schema.RequiredMode.REQUIRED)
  var page: Int = 1,
  @Schema(description = "每頁數量", requiredMode = Schema.RequiredMode.REQUIRED)
  var pageSize: Int = 10
)
