package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema
open class SearchKeyword(
  @Schema(description = "關鍵字")
  var keyword: String? = null,
  @Schema(description = "關鍵字查詢類型")
  var type: SearchKeywordType = SearchKeywordType.STARTS_WITH
)
