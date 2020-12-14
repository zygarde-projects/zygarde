package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import zygarde.data.jpa.search.request.SearchKeyword
import zygarde.data.jpa.search.request.SearchKeywordType

class StringConditionActionImpl<RootEntityType, EntityType>(
  enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  columnName: String
) : ComparableConditionActionImpl<RootEntityType, EntityType, String>(enhancedSearch, columnName), StringConditionAction<RootEntityType, EntityType> {

  override fun keyword(value: SearchKeyword?): EnhancedSearch<RootEntityType> = applyNonNullAction(value?.keyword) { path, keyword ->
    when (value!!.type) {
      SearchKeywordType.CONTAINS -> cb.like(path, "%$keyword%")
      SearchKeywordType.STARTS_WITH -> cb.like(path, "$keyword%")
      SearchKeywordType.ENDS_WITH -> cb.like(path, "%$keyword")
      SearchKeywordType.MATCH -> cb.equal(path, keyword)
    }
  }

  override fun startsWith(value: String?): EnhancedSearch<RootEntityType> {
    return keyword(SearchKeyword(value, SearchKeywordType.STARTS_WITH))
  }

  override fun endsWith(value: String?): EnhancedSearch<RootEntityType> {
    return keyword(SearchKeyword(value, SearchKeywordType.ENDS_WITH))
  }

  override fun contains(value: String?): EnhancedSearch<RootEntityType> {
    return keyword(SearchKeyword(value, SearchKeywordType.CONTAINS))
  }

  override fun containsAny(value: Collection<String>?): EnhancedSearch<RootEntityType> =
    applyNonNullAction(value?.takeIf { it.isNotEmpty() }?.toSet()) { path, keywords ->
      cb.or(
        *keywords.map { keyword -> cb.like(path, "%$keyword%") }.toTypedArray()
      )
    }
}
