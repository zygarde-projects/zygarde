package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

open class KeywordPagingAndSortingRequest : PagingAndSortingRequest() {
  @Schema(description = "關鍵字", required = false)
  var keyword: String? = null
}
