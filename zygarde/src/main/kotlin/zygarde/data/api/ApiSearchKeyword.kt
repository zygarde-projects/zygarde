package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.data.jpa.search.request.SearchKeyword
import zygarde.data.jpa.search.request.SearchKeywordType

@Schema
class ApiSearchKeyword(
  keyword: String? = null,
  type: SearchKeywordType = SearchKeywordType.STARTS_WITH
) : SearchKeyword(keyword, type)
