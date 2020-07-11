package zygarde.data.api

import io.swagger.annotations.ApiModel
import zygarde.data.jpa.search.request.SearchKeyword
import zygarde.data.jpa.search.request.SearchKeywordType

@ApiModel
class ApiSearchKeyword(
  keyword: String? = null,
  type: SearchKeywordType = SearchKeywordType.STARTS_WITH
) : SearchKeyword(keyword, type)
