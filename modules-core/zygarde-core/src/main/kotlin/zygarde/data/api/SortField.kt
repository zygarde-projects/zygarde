package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class SortField(
  @Schema(description = "排序方式")
  var sort: SortDirection? = null,
  @Schema(description = "排序欄位")
  var field: String? = null
)
