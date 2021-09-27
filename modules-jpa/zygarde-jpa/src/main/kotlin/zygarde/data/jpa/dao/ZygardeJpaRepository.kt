package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import javax.persistence.EntityManager

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
}
