package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import zygarde.core.transform.MapToObjectTransformer
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.EntityManager
import javax.persistence.Tuple
import javax.persistence.TypedQuery
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

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
    return queryForProps(searchContent, p).singleResult as P
  }

  @Suppress("UNCHECKED_CAST")
  override fun <P> select(p: KProperty1<T, P>, searchContent: EnhancedSearch<T>.() -> Unit): List<P> {
    return queryForProps(searchContent, p).resultList as List<P>
  }

  override fun <P : Any> select(projection: KClass<P>, searchContent: EnhancedSearch<T>.() -> Unit): List<P> {
    val projectionProps = projection.memberProperties
    val transformer = MapToObjectTransformer(projection)
    return queryForPropsAsTupleList(searchContent, projectionProps).map { tuple ->
      val map = projectionProps.mapIndexed { idx, p -> p.name to tuple.get(idx) }.toMap()
      transformer.transform(map)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun queryForPropsAsTupleList(
    searchContent: EnhancedSearch<T>.() -> Unit,
    props: Collection<KProperty1<*, *>>,
  ): List<Tuple> {
    return queryForProps(searchContent, props).resultList as List<Tuple>
  }

  private fun queryForProps(
    searchContent: EnhancedSearch<T>.() -> Unit,
    vararg props: KProperty1<T, *>,
  ) = queryForProps(searchContent, props.toList())

  @Suppress("UNCHECKED_CAST")
  private fun queryForProps(
    searchContent: EnhancedSearch<T>.() -> Unit,
    props: Collection<KProperty1<*, *>>,
  ): TypedQuery<out Any> {
    val cb = entityManager.criteriaBuilder
    val query = if (props.size == 1) cb.createQuery() else cb.createTupleQuery()
    val root = query.from(domainClass)
    val predicates = mutableListOf<Predicate>()
    val enhancedSearchImpl = EnhancedSearchImpl(
      predicates,
      root,
      query,
      cb
    )
    searchContent.invoke(enhancedSearchImpl)

    if (props.size == 1) {
      (query as CriteriaQuery<Any>).where(cb.and(*predicates.toTypedArray())).select(root.get<Any>(props.first().name))
    } else {
      query.where(cb.and(*predicates.toTypedArray())).multiselect(
        props.map { root.get<Any>(it.name) }
      )
    }

    return entityManager.createQuery(query)
  }
}
