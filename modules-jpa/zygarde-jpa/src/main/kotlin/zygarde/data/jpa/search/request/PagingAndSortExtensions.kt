package zygarde.data.jpa.search.request

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.SortDirection
import zygarde.data.api.SortField

private fun SortDirection?.mapToSpringDataSort(): Sort.Direction {
  return when (this) {
    SortDirection.ASC -> Sort.Direction.ASC
    SortDirection.DESC -> Sort.Direction.DESC
    else -> Sort.Direction.ASC
  }
}

private fun SortField?.toSort(): Sort? {
  return this?.field?.let { f -> Sort.by(sort.mapToSpringDataSort(), f) }
}

fun List<SortField>?.toSpringDataSort(): Sort {
  return this
    ?.takeIf { it.isNotEmpty() }
    ?.mapNotNull { it.toSort() }
    ?.reduceRight { s1, s2 -> s1.and(s2) }
    ?: Sort.unsorted()
}

fun PagingAndSortingRequest.toSpringDataPageRequest(): PageRequest {
  return PageRequest.of(
    paging.page - 1,
    paging.pageSize,
    sorts.toSpringDataSort()
  )
}
