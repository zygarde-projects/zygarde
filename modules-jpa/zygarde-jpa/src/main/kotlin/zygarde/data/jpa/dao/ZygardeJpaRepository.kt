package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import kotlin.reflect.KProperty1

open class ZygardeJpaRepository<T, ID>(
  entityInformation: JpaEntityInformation<T, ID>,
  val entityManager: EntityManager
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), ZygardeEnhancedDao<T, ID> {

  @Suppress("UNCHECKED_CAST")
  override fun <P> selectOne(p: KProperty1<T, P>, searchContent: EnhancedSearch<T>.() -> Unit): P {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery()
    val root = query.from(domainClass)
    val predicates = mutableListOf<Predicate>()
    val enhancedSearchImpl = EnhancedSearchImpl(predicates, root, query, cb)
    searchContent.invoke(enhancedSearchImpl)

    query.where(cb.and(*predicates.toTypedArray())).select(root.get<P>(p.name))

    return entityManager.createQuery(query).singleResult as P
  }
}
