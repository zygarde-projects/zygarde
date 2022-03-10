package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

open class PagingAndSortingRequest {
  @Schema(description = "分頁", required = true)
  var paging: PagingRequest = PagingRequest()

  @Schema(description = "排序", required = false)
  var sorts: List<SortField>? = null
}
