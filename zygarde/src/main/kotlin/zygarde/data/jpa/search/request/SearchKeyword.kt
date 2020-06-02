package zygarde.data.jpa.search.request

open class SearchKeyword(
  var keyword: String? = null,
  var type: SearchKeywordType = SearchKeywordType.STARTS_WITH
)
