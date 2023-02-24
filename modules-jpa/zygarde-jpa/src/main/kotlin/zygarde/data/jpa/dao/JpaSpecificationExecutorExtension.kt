package zygarde.data.jpa.dao

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import zygarde.data.jpa.search.request.toSpringDataPageRequest
import zygarde.data.jpa.search.request.toSpringDataSort
import jakarta.persistence.criteria.Predicate

private fun <T> buildSpec(searchContent: EnhancedSearch<T>.() -> Unit): Specification<T> {
  return Specification<T> { root, query, cb ->
    val predicates = mutableListOf<Predicate>()
    val enhancedSearchImpl = EnhancedSearchImpl(predicates, root, query, cb)
    searchContent.invoke(enhancedSearchImpl)
    if (enhancedSearchImpl.orders.isNotEmpty()) {
      query.orderBy(enhancedSearchImpl.orders)
    }
    cb.and(*predicates.toTypedArray())
  }
}

fun <T> JpaSpecificationExecutor<T>.search(searchContent: EnhancedSearch<T>.() -> Unit): List<T> {
  return findAll(buildSpec(searchContent))
}

fun <T> JpaSpecificationExecutor<T>.search(sorts: List<SortField>?, searchContent: EnhancedSearch<T>.() -> Unit): List<T> {
  return sorts?.let { findAll(buildSpec(searchContent), it.toSpringDataSort()) } ?: search(searchContent)
}

/**
 * search with limit results, there's a limitation that spring-data-jpa will perform a extra count query
 */
fun <T> JpaSpecificationExecutor<T>.search(searchContent: EnhancedSearch<T>.() -> Unit, limit: Int): List<T> {
  return findAll(buildSpec(searchContent), PageRequest.of(0, limit)).content
}

fun <T> JpaSpecificationExecutor<T>.searchCount(searchContent: EnhancedSearch<T>.() -> Unit): Long {
  return count(buildSpec(searchContent))
}

fun <T> JpaSpecificationExecutor<T>.searchOne(searchContent: EnhancedSearch<T>.() -> Unit): T? {
  return findOne(buildSpec(searchContent)).let { if (it.isPresent) it.get() else null }
}

fun <T> JpaSpecificationExecutor<T>.searchPage(
  req: PagingAndSortingRequest,
  searchContent: EnhancedSearch<T>.() -> Unit
): Page<T> {
  return findAll(buildSpec(searchContent), req.toSpringDataPageRequest())
}

fun <T> ZygardeEnhancedDao<T, *>.remove(searchContent: EnhancedSearch<T>.() -> Unit) {
  deleteBySpec(buildSpec(searchContent))
}
