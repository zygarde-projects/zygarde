package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl

open class ComparableConditionActionImpl<RootEntityType, EntityType, FieldType : Comparable<FieldType>>(
  enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  columnName: String
) : ConditionActionImpl<RootEntityType, EntityType, FieldType>(enhancedSearch, columnName),
  ComparableConditionAction<RootEntityType, EntityType, FieldType> {

  override fun gt(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.greaterThan(path, v)
  }

  override fun gte(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.greaterThanOrEqualTo(path, v)
  }

  override fun lt(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.lessThan(path, v)
  }

  override fun lte(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.lessThanOrEqualTo(path, v)
  }
}
