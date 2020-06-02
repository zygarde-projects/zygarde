package zygarde.data.jpa.search.request

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * @author leo
 */
data class PagingRequest(
  var page: Int = 1,
  var pageSize: Int = 10
)

fun PagingRequest.toPageRequest(sort: Sort): PageRequest {
  return PageRequest.of(
    page - 1,
    pageSize,
    sort
  )
}
