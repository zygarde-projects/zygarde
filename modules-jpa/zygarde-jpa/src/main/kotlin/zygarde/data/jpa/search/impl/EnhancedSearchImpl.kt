package zygarde.data.jpa.search.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.impl.ComparableConditionActionImpl
import zygarde.data.jpa.search.action.impl.ConcatStringConditionImpl
import zygarde.data.jpa.search.action.impl.ConditionActionImpl
import zygarde.data.jpa.search.action.impl.StringConditionActionImpl
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Fetch
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

class EnhancedSearchImpl<EntityType>(
  val predicates: MutableList<Predicate>,
  val root: Root<EntityType>,
  val query: CriteriaQuery<*>,
  val cb: CriteriaBuilder,
  val orders: MutableList<Order> = mutableListOf(),
  val joinMap: MutableMap<String, Join<Any, Any>> = mutableMapOf(),
  val fetchMap: MutableMap<String, Fetch<Any, Any>> = mutableMapOf(),
) : EnhancedSearch<EntityType> {

  override fun <FieldType> field(fieldName: String): ConditionAction<EntityType, EntityType, FieldType> {
    return ConditionActionImpl(this, fieldName)
  }

  override fun <FieldType : Comparable<FieldType>> comparableField(fieldName: String): ComparableConditionAction<EntityType, EntityType, FieldType> {
    return ComparableConditionActionImpl(this, fieldName)
  }

  override fun stringField(fieldName: String): StringConditionAction<EntityType, EntityType> {
    return StringConditionActionImpl(this, fieldName)
  }

  override fun <FieldType> join(fieldName: String): ConditionAction<EntityType, EntityType, FieldType> {
    return ConditionActionImpl(this, fieldName, true)
  }

  override fun concat(vararg stringFields: StringConditionAction<EntityType, *>): StringConditionAction<EntityType, EntityType> {
    return ConcatStringConditionImpl(this, stringFields.toList())
  }

  override fun or(searchContent: (enhancedSearch: EnhancedSearch<EntityType>) -> Unit): EnhancedSearch<EntityType> {
    val predicatesForOr = nestedPredicates(searchContent)
    return this.apply {
      if (predicatesForOr.isNotEmpty()) {
        predicates.add(
          cb.or(*predicatesForOr.toTypedArray())
        )
      }
    }
  }

  override fun and(searchContent: (enhancedSearch: EnhancedSearch<EntityType>) -> Unit): EnhancedSearch<EntityType> {
    val predicatesForAnd = nestedPredicates(searchContent)
    return this.apply {
      if (predicatesForAnd.isNotEmpty()) {
        predicates.add(
          cb.and(*predicatesForAnd.toTypedArray())
        )
      }
    }
  }

  private fun nestedPredicates(searchContent: (enhancedSearch: EnhancedSearch<EntityType>) -> Unit): List<Predicate> {
    val predicates = mutableListOf<Predicate>()
    searchContent.invoke(
      EnhancedSearchImpl(
        predicates,
        root,
        query,
        cb
      )
    )
    return predicates
  }

  override fun <FieldType : Comparable<FieldType>> rangeOverlap(
    fieldStartFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    fieldEndFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    from: FieldType?,
    to: FieldType?,
  ) {
    if (from == null || to == null) return
    or {
      it.and {
        this.fieldStartFn() lte from
        this.fieldEndFn() gte from
      }
      it.and {
        this.fieldStartFn() lte to
        this.fieldEndFn() gte to
      }
      it.and {
        this.fieldStartFn() lte from
        this.fieldEndFn() gte to
      }
      it.and {
        this.fieldStartFn() gte from
        this.fieldEndFn() lte to
      }
    }
  }

  override fun distinct() {
    this.query.distinct(true)
  }
}
