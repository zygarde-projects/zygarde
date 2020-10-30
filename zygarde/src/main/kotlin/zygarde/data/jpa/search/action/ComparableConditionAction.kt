package zygarde.data.jpa.search.action

import zygarde.data.jpa.search.EnhancedSearch

interface ComparableConditionAction<RootEntityType, EntityType, FieldType : Comparable<FieldType>> :
  ConditionAction<RootEntityType, EntityType, FieldType> {
  infix fun gt(value: FieldType?): EnhancedSearch<RootEntityType>
  infix fun gte(value: FieldType?): EnhancedSearch<RootEntityType>
  infix fun lt(value: FieldType?): EnhancedSearch<RootEntityType>
  infix fun lte(value: FieldType?): EnhancedSearch<RootEntityType>
  infix fun gt(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>)
  infix fun gte(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>)
  infix fun lt(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>)
  infix fun lte(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>)
}
