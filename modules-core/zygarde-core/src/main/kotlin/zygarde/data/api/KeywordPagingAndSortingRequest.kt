package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

open class KeywordPagingAndSortingRequest : PagingAndSortingRequest() {
  @Schema(description = "關鍵字", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  var keyword: String? = null
}
