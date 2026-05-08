package zygarde.test.dao

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortField
import zygarde.data.jpa.dao.ZygardeEnhancedDao
import zygarde.data.jpa.search.EnhancedSearch
import zygarde.data.jpa.search.SearchSpecBuilder
import zygarde.data.jpa.search.request.toSpringDataPageRequest
import zygarde.data.jpa.search.request.toSpringDataSort

fun <T> JpaSpecificationExecutor<T>.search(searchContent: EnhancedSearch<T>.() -> Unit): List<T> {
  return findAll(SearchSpecBuilder.buildSpec(searchContent))
}

fun <T> JpaSpecificationExecutor<T>.search(sorts: List<SortField>?, searchContent: EnhancedSearch<T>.() -> Unit): List<T> {
  return sorts?.let { findAll(SearchSpecBuilder.buildSpec(searchContent), it.toSpringDataSort()) } ?: search(searchContent)
}

fun <T> JpaSpecificationExecutor<T>.search(searchContent: EnhancedSearch<T>.() -> Unit, limit: Int): List<T> {
  return findAll(SearchSpecBuilder.buildSpec(searchContent), PageRequest.of(0, limit)).content
}

fun <T> JpaSpecificationExecutor<T>.searchCount(searchContent: EnhancedSearch<T>.() -> Unit): Long {
  return count(SearchSpecBuilder.buildSpec(searchContent))
}

fun <T> JpaSpecificationExecutor<T>.searchOne(searchContent: EnhancedSearch<T>.() -> Unit): T? {
  return findOne(SearchSpecBuilder.buildSpec(searchContent)).let { if (it.isPresent) it.get() else null }
}

fun <T> JpaSpecificationExecutor<T>.searchOneOrThrow(
  errorCode: ErrorCode,
  searchContent: EnhancedSearch<T>.() -> Unit
): T {
  return searchOne(searchContent) ?: throw BusinessException(errorCode)
}

fun <T> JpaSpecificationExecutor<T>.searchPage(
  req: PagingAndSortingRequest,
  searchContent: EnhancedSearch<T>.() -> Unit
): Page<T> {
  return findAll(SearchSpecBuilder.buildSpec(searchContent), req.toSpringDataPageRequest())
}

fun <T> ZygardeEnhancedDao<T, *>.remove(searchContent: EnhancedSearch<T>.() -> Unit): Long {
  return delete(SearchSpecBuilder.buildSpec(searchContent))
}
