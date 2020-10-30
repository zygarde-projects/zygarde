package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

open class ConditionActionImpl<RootEntityType, EntityType, FieldType>(
  private val enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  private val columnName: String
) : ConditionAction<RootEntityType, EntityType, FieldType> {

  override fun <AnotherFieldType> field(fieldName: String): ConditionAction<RootEntityType, FieldType, AnotherFieldType> {
    return ConditionActionImpl<RootEntityType, FieldType, AnotherFieldType>(
      enhancedSearch,
      "$columnName.$fieldName"
    )
  }

  override fun <AnotherFieldType : Comparable<AnotherFieldType>> comparableField(
    fieldName: String
  ): ComparableConditionAction<RootEntityType, FieldType, AnotherFieldType> {
    return ComparableConditionActionImpl<RootEntityType, FieldType, AnotherFieldType>(
      enhancedSearch,
      "$columnName.$fieldName"
    )
  }

  override fun stringField(fieldName: String): StringConditionAction<RootEntityType, FieldType> {
    return StringConditionActionImpl<RootEntityType, FieldType>(enhancedSearch, "$columnName.$fieldName")
  }

  override fun <AnotherFieldType> field(
    searchable: Searchable<FieldType, AnotherFieldType>
  ): ConditionAction<RootEntityType, FieldType, AnotherFieldType> = field(searchable.fieldName())

  override fun <AnotherFieldType : Comparable<AnotherFieldType>> field(
    searchable: Searchable<FieldType, AnotherFieldType>
  ): ComparableConditionAction<RootEntityType, FieldType, AnotherFieldType> = comparableField(searchable.fieldName())

  override fun field(searchable: Searchable<FieldType, String>): StringConditionAction<RootEntityType, FieldType> =
    stringField(searchable.fieldName())

  override fun join(joinType: JoinType) {
    if (enhancedSearch.query.resultType.canonicalName != "java.lang.Long") {
      val splited = columnName.split(".")
      splited.takeLast(splited.size - 1)
        .fold(
          enhancedSearch.root.fetch<EntityType, FieldType>(splited.first(), joinType),
          { fetch, column -> fetch.fetch(column, joinType) }
        )
    }
  }

  override fun eq(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.equal(path, v)
  }

  override fun notEq(value: FieldType?): EnhancedSearch<RootEntityType> = applyNonNullAction(value) { path, v ->
    cb.notEqual(path, v)
  }

  override fun inList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType> =
    applyNonNullAction(values?.takeIf { it.isNotEmpty() }) { path, v ->
      path.`in`(v)
    }

  override fun notInList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType> =
    applyNonNullAction(values?.takeIf { it.isNotEmpty() }) { path, v ->
      path.`in`(v).not()
    }

  override fun eq(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>) = applyThisAndAnother(anotherAction) { l, r ->
    cb.equal(l, r)
  }

  override fun notEq(anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>) = applyThisAndAnother(anotherAction) { l, r ->
    cb.notEqual(l, r)
  }

  override fun isNotNull(): EnhancedSearch<RootEntityType> = enhancedSearch.apply {
    predicates.add(cb.isNotNull(root.columnNameToPath(columnName)))
  }

  override fun isNull(): EnhancedSearch<RootEntityType> = enhancedSearch.apply {
    predicates.add(cb.isNull(root.columnNameToPath(columnName)))
  }

  override fun asc(): EnhancedSearch<RootEntityType> =
    enhancedSearch.apply { orders.add(cb.asc(root.columnNameToPath(columnName))) }

  override fun desc(): EnhancedSearch<RootEntityType> =
    enhancedSearch.apply { orders.add(cb.desc(root.columnNameToPath(columnName))) }

  protected fun applyThisAndAnother(
    anotherAction: ConditionAction<RootEntityType, EntityType, FieldType>,
    block: EnhancedSearchImpl<RootEntityType>.(thisFieldPath: Path<FieldType>, anotherPath: Path<FieldType>) -> Unit
  ) {
    if (anotherAction is ConditionActionImpl) {
      block.invoke(
        enhancedSearch,
        enhancedSearch.root.columnNameToPath(columnName),
        anotherAction.enhancedSearch.root.columnNameToPath(anotherAction.columnName)
      )
    } else {
      throw IllegalArgumentException("another action is not subclass of ConditionActionImpl")
    }
  }

  protected fun Root<RootEntityType>.columnNameToPath(columnName: String): Path<FieldType> {
    val splited = columnName.split(".")
    return splited.takeLast(splited.size - 1)
      .fold(
        this.get<FieldType>(splited.first()),
        { path, column -> path.get(column) }
      )
  }

  protected fun <T> applyNonNullAction(
    value: T?,
    block: EnhancedSearchImpl<RootEntityType>.(path: Path<FieldType>, v: T) -> Predicate
  ) =
    enhancedSearch.apply {
      value?.let { predicates.add(block.invoke(this, root.columnNameToPath(columnName), it)) }
    }
}
