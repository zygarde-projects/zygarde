package zygarde.data.jpa.search.impl

import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.impl.ComparableConditionActionImpl
import zygarde.data.jpa.search.action.impl.ConcatStringConditionImpl
import zygarde.data.jpa.search.action.impl.ConditionActionImpl
import zygarde.data.jpa.search.action.impl.StringConditionActionImpl
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Fetch
import javax.persistence.criteria.Join
import javax.persistence.criteria.Order
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

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
    dateFieldStartFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    dateFieldEndFn: EnhancedSearch<EntityType>.() -> ComparableConditionAction<EntityType, *, FieldType>,
    from: FieldType?,
    to: FieldType?,
  ) {
    if (from == null || to == null) return
    or {
      it.and {
        this.dateFieldStartFn() lte from
        this.dateFieldEndFn() gte from
      }
      it.and {
        this.dateFieldStartFn() lte to
        this.dateFieldEndFn() gte to
      }
      it.and {
        this.dateFieldStartFn() lte from
        this.dateFieldEndFn() gte to
      }
      it.and {
        this.dateFieldStartFn() gte from
        this.dateFieldEndFn() lte to
      }
    }
  }

  override fun distinct() {
    this.query.distinct(true)
  }
}
