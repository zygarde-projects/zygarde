package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema
import zygarde.data.search.SearchKeyword
import zygarde.data.search.SearchKeywordType

@Schema
class ApiSearchKeyword(
  keyword: String? = null,
  type: SearchKeywordType = SearchKeywordType.STARTS_WITH
) : SearchKeyword(keyword, type)
