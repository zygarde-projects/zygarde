package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import zygarde.data.search.SearchKeyword
import zygarde.data.search.SearchKeywordType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate

open class StringConditionActionImpl<RootEntityType, EntityType>(
  val enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  val columnName: String,
  val pathExpressionProcessor: (cb: CriteriaBuilder, exp: Expression<String>) -> Expression<String> = { _, exp -> exp },
) : ComparableConditionActionImpl<RootEntityType, EntityType, String>(enhancedSearch, columnName), StringConditionAction<RootEntityType, EntityType> {
  companion object {
    private const val ESCAPE_CHAR = '\\'

    fun escapeLike(value: String): String = value
      .replace("\\", "\\\\")
      .replace("%", "\\%")
      .replace("_", "\\_")
  }

  override fun keyword(value: SearchKeyword?): EnhancedSearch<RootEntityType> = applyNonNullAction(value?.keyword) { path, keyword ->
    when (value!!.type) {
      SearchKeywordType.CONTAINS -> cb.like(path, "%${escapeLike(keyword)}%", ESCAPE_CHAR)
      SearchKeywordType.STARTS_WITH -> cb.like(path, "${escapeLike(keyword)}%", ESCAPE_CHAR)
      SearchKeywordType.ENDS_WITH -> cb.like(path, "%${escapeLike(keyword)}", ESCAPE_CHAR)
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
        *keywords.map { keyword -> cb.like(path, "%${escapeLike(keyword)}%", ESCAPE_CHAR) }.toTypedArray()
      )
    }

  override fun eqIgnoreCase(value: String?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.equal(cb.lower(path), v.lowercase())
  }

  override fun containsIgnoreCase(value: String?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.like(cb.lower(path), "%${escapeLike(v.lowercase())}%", ESCAPE_CHAR)
  }

  override fun startsWithIgnoreCase(value: String?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.like(cb.lower(path), "${escapeLike(v.lowercase())}%", ESCAPE_CHAR)
  }

  override fun endsWithIgnoreCase(value: String?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.like(cb.lower(path), "%${escapeLike(v.lowercase())}", ESCAPE_CHAR)
  }

  override fun lower(): StringConditionAction<RootEntityType, EntityType> {
    return StringConditionActionImpl(enhancedSearch, columnName) { cb, exp ->
      cb.lower(exp)
    }
  }

  override fun upper(): StringConditionAction<RootEntityType, EntityType> {
    return StringConditionActionImpl(enhancedSearch, columnName) { cb, exp ->
      cb.upper(exp)
    }
  }

  override fun trim(): StringConditionAction<RootEntityType, EntityType> {
    return StringConditionActionImpl(enhancedSearch, columnName) { cb, exp ->
      cb.trim(exp)
    }
  }

  override fun <T> applyNonNullAction(
    value: T?,
    block: EnhancedSearchImpl<RootEntityType>.(path: Expression<String>, v: T) -> Predicate
  ): EnhancedSearchImpl<RootEntityType> {
    return super.applyNonNullAction(value) { path, v ->
      block.invoke(this, pathExpressionProcessor(cb, path), v)
    }
  }
}
