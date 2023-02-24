package zygarde.data.jpa.search.action

import zygarde.data.jpa.search.EnhancedSearch
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.JoinType
import kotlin.reflect.KProperty1

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

  fun <NestedFieldType> field(
    property: KProperty1<FieldType, NestedFieldType?>
  ): ConditionAction<RootEntityType, FieldType, NestedFieldType> = field(property.name)

  fun field(
    property: KProperty1<FieldType, String?>
  ): StringConditionAction<RootEntityType, FieldType> = stringField(property.name)

  fun <AnotherFieldType : Comparable<AnotherFieldType>> field(
    property: KProperty1<FieldType, AnotherFieldType?>
  ): ComparableConditionAction<RootEntityType, FieldType, AnotherFieldType> = comparableField(property.name)

  fun join(joinType: JoinType = JoinType.INNER)

  infix fun eq(value: FieldType?): EnhancedSearch<RootEntityType>

  infix fun notEq(value: FieldType?): EnhancedSearch<RootEntityType>

  infix fun inList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType>

  infix fun notInList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType>

  infix fun eq(anotherAction: ConditionAction<*, *, FieldType>)

  infix fun notEq(anotherAction: ConditionAction<*, *, FieldType>)

  fun isNotNull(): EnhancedSearch<RootEntityType>

  fun isNull(): EnhancedSearch<RootEntityType>

  fun asc(): EnhancedSearch<RootEntityType>

  fun desc(): EnhancedSearch<RootEntityType>

  fun asExpression(): Expression<FieldType>
}
