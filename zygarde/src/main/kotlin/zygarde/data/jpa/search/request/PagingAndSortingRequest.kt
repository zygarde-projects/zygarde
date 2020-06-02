package zygarde.data.jpa.search.request

open class PagingAndSortingRequest {
  var paging: PagingRequest = PagingRequest()
  var sorting: SortingRequest? = null
}
