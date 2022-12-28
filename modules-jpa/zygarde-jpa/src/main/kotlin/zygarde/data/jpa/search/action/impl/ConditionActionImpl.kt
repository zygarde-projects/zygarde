package zygarde.data.jpa.search.action.impl

import org.hibernate.query.criteria.internal.path.SingularAttributePath
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.criteria.Expression
import javax.persistence.criteria.Join
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

open class ConditionActionImpl<RootEntityType, EntityType, FieldType>(
  private val enhancedSearch: EnhancedSearchImpl<RootEntityType>,
  private val columnName: String,
  private val join: Boolean = false,
  private val isCountQuery: Boolean = enhancedSearch.query.resultType.canonicalName == "java.lang.Long",
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

  override fun join(joinType: JoinType) {
    val split = columnName.split(".")
    var colName = split.first()
    if (isCountQuery) {
      split.takeLast(split.size - 1)
        .fold(
          enhancedSearch.joinMap.getOrPut(split.first()) {
            enhancedSearch.root.join(split.first(), joinType)
          }
        ) { join, foldedColumn ->
          colName = "$colName.$foldedColumn"
          enhancedSearch.joinMap.getOrPut(
            colName,
          ) {
            join.join(foldedColumn, joinType)
          }
        }
    } else {
      split.takeLast(split.size - 1)
        .fold(
          enhancedSearch.fetchMap.getOrPut(
            colName,
          ) {
            enhancedSearch.root.fetch(colName, joinType)
          }
        ) { fetch, foldedColumn ->
          colName = "$colName.$foldedColumn"
          enhancedSearch.fetchMap.getOrPut(
            colName,
          ) {
            fetch.fetch(foldedColumn, joinType)
          }
        }
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
      if (v.size > 500) {
        cb.or(*v.chunked(500).map { chunkedValues -> path.`in`(chunkedValues) }.toTypedArray())
      } else {
        path.`in`(v)
      }
    }

  override fun notInList(values: Collection<FieldType>?): EnhancedSearch<RootEntityType> =
    applyNonNullAction(values?.takeIf { it.isNotEmpty() }) { path, v ->
      if (v.size > 500) {
        cb.and(*v.chunked(500).map { chunkedValues -> path.`in`(chunkedValues).not() }.toTypedArray())
      } else {
        path.`in`(v).not()
      }
    }

  override fun eq(anotherAction: ConditionAction<*, *, FieldType>) = applyThisAndAnother(anotherAction) { l, r ->
    cb.equal(l, r)
  }

  override fun notEq(anotherAction: ConditionAction<*, *, FieldType>) = applyThisAndAnother(anotherAction) { l, r ->
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
    anotherAction: ConditionAction<*, *, FieldType>,
    block: EnhancedSearchImpl<*>.(thisFieldPath: Path<FieldType>, anotherPath: Path<FieldType>) -> Predicate
  ) {
    if (anotherAction is ConditionActionImpl) {
      enhancedSearch.predicates.add(
        block.invoke(
          enhancedSearch,
          enhancedSearch.root.columnNameToPath(columnName),
          anotherAction.enhancedSearch.root.columnNameToPath(anotherAction.columnName)
        )
      )
    } else {
      throw IllegalArgumentException("another action is not subclass of ConditionActionImpl")
    }
  }

  override fun asExpression(): Expression<FieldType> {
    return enhancedSearch.root.columnNameToPath(columnName)
  }

  @Suppress("UNCHECKED_CAST")
  protected fun Root<*>.columnNameToPath(columnName: String): Path<FieldType> {
    val splited = columnName.split(".")
    if (splited.size == 1) {
      if (join) {
        return enhancedSearch.root.join<EntityType, FieldType>(columnName)
      }
      return this.get(splited.first())
    }
    if (isCountQuery) {
      val initialPath = enhancedSearch.root.get<Any>(splited[0])
      if (initialPath is SingularAttributePath<*>) {
        return splited.takeLast(splited.size - 1).fold(initialPath as Path<Any>) { join, foldedColumn ->
          join.get<Any>(foldedColumn)
        } as Path<FieldType>
      }
    }

    val fetch = enhancedSearch.fetchMap[splited.take(splited.size - 1).joinToString(".")]
    if (fetch != null && fetch is Join<*, *>) {
      return fetch.get<FieldType>(splited.last())
    }
    var joinPath = splited[0]
    var currentJoin = enhancedSearch.joinMap.getOrPut(joinPath) {
      this.join(joinPath, JoinType.LEFT)
    }
    splited.forEachIndexed { idx, column ->
      val isLast = idx == splited.size - 1
      if (idx > 0 && !isLast) {
        joinPath = "$joinPath.$column"
        currentJoin = enhancedSearch.joinMap.getOrPut(joinPath) {
          currentJoin.join(column, JoinType.LEFT)
        }
      }
    }
    return currentJoin.get(splited.last())
  }

  protected open fun <T> applyNonNullAction(
    value: T?,
    block: EnhancedSearchImpl<RootEntityType>.(path: Expression<FieldType>, v: T) -> Predicate
  ) =
    enhancedSearch.apply {
      value?.let { predicates.add(block.invoke(this, asExpression(), it)) }
    }
}
