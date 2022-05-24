package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.Searchable
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.EntityManager
import javax.persistence.criteria.Predicate

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
  override fun <P> selectOne(p: Searchable<T, P>, searchContent: EnhancedSearch<T>.() -> Unit): P {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery()
    val root = query.from(domainClass)
    val predicates = mutableListOf<Predicate>()
    val enhancedSearchImpl = EnhancedSearchImpl(predicates, root, query, cb)
    searchContent.invoke(enhancedSearchImpl)

    query.where(cb.and(*predicates.toTypedArray())).select(root.get<P>(p.fieldName()))

    return entityManager.createQuery(query).singleResult as P
  }
}
