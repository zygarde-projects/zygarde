package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate

class ConcatStringConditionImpl<RootEntityType, EntityType>(
  val enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  val stringFields: List<StringConditionAction<RootEntityType, *>>
) : StringConditionActionImpl<RootEntityType, EntityType>(enhancedSearch, "") {
  override fun <T> applyNonNullAction(
    value: T?,
    block: EnhancedSearchImpl<RootEntityType>.(path: Expression<String>, v: T) -> Predicate
  ): EnhancedSearchImpl<RootEntityType> {
    var exp: Expression<String>? = null
    stringFields.reduce { l, r ->
      exp = enhancedSearch.cb.concat(l.asExpression(), r.asExpression())
      r
    }
    return enhancedSearch.apply {
      exp?.also { expression ->
        value?.let { predicates.add(block.invoke(this, expression, it)) }
      }
    }
  }
}
