package zygarde.test.api

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import zygarde.data.api.OpenApiSortableFields
import zygarde.data.api.PagingAndSortingRequest

@OpenApiSortableFields("id", "title", "author.name")
class SearchBookReq : PagingAndSortingRequest()

@RestController
class SortableFieldsTestApi {
  @PostMapping("/sortable-books/search")
  fun search(
    @RequestBody req: SearchBookReq,
  ): SearchBookReq = req
}
