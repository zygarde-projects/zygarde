package zygarde.data.jpa.search

import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.search.range.SearchRangeOverlap
import kotlin.reflect.KProperty1

interface EnhancedSearch<EntityType> {

  fun <FieldType> field(
    fieldName: String
  ): ConditionAction<EntityType, EntityType, FieldType>

  fun stringField(
    fieldName: String
  ): StringConditionAction<EntityType, EntityType>

  fun <FieldType : Comparable<FieldType>> comparableField(
    fieldName: String
  ): ComparableConditionAction<EntityType, EntityType, FieldType>

  fun <FieldType> field(
    property: KProperty1<EntityType, FieldType?>
  ): ConditionAction<EntityType, EntityType, FieldType> = field(property.name)

  fun field(
    property: KProperty1<EntityType, String?>
  ): StringConditionAction<EntityType, EntityType> = stringField(property.name)

  fun <FieldType : Comparable<FieldType>> field(
    property: KProperty1<EntityType, FieldType?>
  ): ComparableConditionAction<EntityType, EntityType, FieldType> = comparableField(property.name)

  fun <FieldType> join(
    fieldName: String
  ): ConditionAction<EntityType, EntityType, FieldType>

  fun <FieldType : Comparable<FieldType>> rangeOverlap(
    fieldStartFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    fieldEndFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    from: FieldType?,
    to: FieldType?,
  )

  fun <EntityType, FieldType : Comparable<FieldType>, RangeOverlap : SearchRangeOverlap<FieldType>> EnhancedSearch<EntityType>.rangeOverlap(
    fieldStartFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    fieldEndFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    range: RangeOverlap,
  ) {
    rangeOverlap(fieldStartFn, fieldEndFn, range.start, range.end)
  }

  fun concat(vararg stringFields: StringConditionAction<EntityType, *>): StringConditionAction<EntityType, EntityType>

  fun or(searchContent: EnhancedSearch<EntityType>.() -> Unit): EnhancedSearch<EntityType>

  fun and(searchContent: EnhancedSearch<EntityType>.() -> Unit): EnhancedSearch<EntityType>

  fun distinct()
}
