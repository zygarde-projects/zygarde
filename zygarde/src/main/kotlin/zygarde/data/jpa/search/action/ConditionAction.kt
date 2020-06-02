package zygarde.data.jpa.search.action

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable

interface ConditionAction<RootEntityType, EntityType, FieldType> {

  fun <AnotherFieldType> field(
    fieldName: String
  ): ConditionAction<RootEntityType, FieldType, AnotherFieldType>

  fun <AnotherFieldType : Comparable<AnotherFieldType>> comparableField(
    fieldName: String
  ): ComparableConditionAction<RootEntityType, FieldType, AnotherFieldType>

  fun stringField(
    fieldName: String
  ): StringConditionAction<RootEntityType, FieldType>

  fun <AnotherFieldType> field(
    searchable: Searchable<FieldType, AnotherFieldType>
  ): ConditionAction<RootEntityType, FieldType, AnotherFieldType>

  fun <AnotherFieldType : Comparable<AnotherFieldType>> field(
    searchable: Searchable<FieldType, AnotherFieldType>
  ): ComparableConditionAction<RootEntityType, FieldType, AnotherFieldType>

  fun field(
    searchable: Searchable<FieldType, String>
  ): StringConditionAction<RootEntityType, FieldType>

  fun join()

  infix fun eq(value: FieldType?): EnhancedSearch<RootEntityType>

  infix fun notEq(value: FieldType?): EnhancedSearch<RootEntityType>

  infix fun inList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType>

  infix fun notInList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType>

  fun isNotNull(): EnhancedSearch<RootEntityType>

  fun isNull(): EnhancedSearch<RootEntityType>

  fun asc(): EnhancedSearch<RootEntityType>

  fun desc(): EnhancedSearch<RootEntityType>
}
