package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

open class PagingAndSortingRequest {
  @Schema(description = "分頁", requiredMode = Schema.RequiredMode.REQUIRED)
  var paging: PagingRequest = PagingRequest()

  @Schema(description = "排序", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var sorts: List<SortField>? = null
}
