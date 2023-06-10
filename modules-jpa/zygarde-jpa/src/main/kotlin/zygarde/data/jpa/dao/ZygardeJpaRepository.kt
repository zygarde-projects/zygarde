package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.EntityManager
import javax.persistence.TypedQuery
import javax.persistence.criteria.Predicate
import kotlin.reflect.KProperty1

open class ZygardeJpaRepository<T, ID>(
  entityInformation: JpaEntityInformation<T, ID>,
  val entityManager: EntityManager
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), ZygardeEnhancedDao<T, ID> {

  override fun delete(spec: Specification<T>) {
    val cb = entityManager.criteriaBuilder
    val criteriaDelete = cb.createCriteriaDelete(domainClass)
    val root = criteriaDelete.from(domainClass)
    val query = cb.createQuery()
    criteriaDelete.where(spec.toPredicate(root, query, cb))
    entityManager.createQuery(criteriaDelete).executeUpdate()
  }

  @Suppress("UNCHECKED_CAST")
  override fun <P> selectOne(p: KProperty1<T, P>, searchContent: EnhancedSearch<T>.() -> Unit): P {
    return queryForProp(searchContent, p).singleResult as P
  }

  @Suppress("UNCHECKED_CAST")
  override fun <P> select(p: KProperty1<T, P>, searchContent: EnhancedSearch<T>.() -> Unit): List<P> {
    return queryForProp(searchContent, p).resultList as List<P>
  }

  private fun <P> queryForProp(
    searchContent: EnhancedSearch<T>.() -> Unit,
    p: KProperty1<T, P>
  ): TypedQuery<Any> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery()
    val root = query.from(domainClass)
    val predicates = mutableListOf<Predicate>()
    val enhancedSearchImpl = EnhancedSearchImpl(predicates, root, query, cb)
    searchContent.invoke(enhancedSearchImpl)

    query.where(cb.and(*predicates.toTypedArray())).select(root.get<P>(p.name))

    return entityManager.createQuery(query)
  }
}
