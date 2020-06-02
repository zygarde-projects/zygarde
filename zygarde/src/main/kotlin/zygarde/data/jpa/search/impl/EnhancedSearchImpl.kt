package zygarde.data.jpa.search.impl

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Order
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.impl.ComparableConditionActionImpl
import zygarde.data.jpa.search.action.impl.ConditionActionImpl
import zygarde.data.jpa.search.action.impl.StringConditionActionImpl

class EnhancedSearchImpl<EntityType>(
  val predicates: MutableList<Predicate>,
  val root: Root<EntityType>,
  private val query: CriteriaQuery<*>,
  val cb: CriteriaBuilder,
  val orders: MutableList<Order> = mutableListOf()
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

  override fun <FieldType> field(
    searchable: Searchable<EntityType, FieldType>
  ): ConditionAction<EntityType, EntityType, FieldType> {
    return ConditionActionImpl(this, searchable.fieldName())
  }

  override fun <FieldType : Comparable<FieldType>> field(
    searchable: Searchable<EntityType, FieldType>
  ): ComparableConditionAction<EntityType, EntityType, FieldType> {
    return ComparableConditionActionImpl(this, searchable.fieldName())
  }

  override fun field(searchable: Searchable<EntityType, String>): StringConditionAction<EntityType, EntityType> {
    return StringConditionActionImpl(this, searchable.fieldName())
  }

  override fun or(searchContent: (enhancedSearch: EnhancedSearch<EntityType>) -> Unit): EnhancedSearch<EntityType> {
    val predicatesForOr = mutableListOf<Predicate>()
    searchContent.invoke(
      EnhancedSearchImpl(
        predicatesForOr,
        root,
        query,
        cb
      )
    )
    return this.apply {
      predicates.add(
        cb.or(*predicatesForOr.toTypedArray())
      )
    }
  }
}
