package zygarde.data.jpa.dao

import javax.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.data.jpa.search.request.SortingRequest
import zygarde.data.jpa.search.request.asSort
import zygarde.data.jpa.search.request.toPageRequest

private fun <T> buildSpec(searchContent: EnhancedSearch<T>.() -> Unit): Specification<T> {
  val predicates = mutableListOf<Predicate>()
  return Specification<T> { root, query, cb ->
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

fun <T> JpaSpecificationExecutor<T>.search(searchContent: EnhancedSearch<T>.() -> Unit, sorting: SortingRequest): List<T> {
  return findAll(buildSpec(searchContent), sorting.asSort())
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
  return findAll(buildSpec(searchContent), req.paging.toPageRequest(req.sorting.asSort()))
}
