package zygarde.data.jpa.search.request

import org.springframework.data.domain.Sort

/**
 * @author leo
 */
data class SortingRequest(
  var sort: Sort.Direction = Sort.Direction.DESC,
  var sortFields: List<String> = emptyList()
)

fun SortingRequest?.asSort(): Sort {
  if (this != null && sortFields.isNotEmpty()) {
    return Sort.by(sort, *sortFields.toTypedArray())
  }
  return Sort.unsorted()
}
