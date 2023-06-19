package codegen.spec

import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.ApiPathVariable
import zygarde.codegen.GenApi
import zygarde.codegen.ZyApi

data class CurrentUser(val id: Int)

@ZyApi(
  [
    GenApi(
      method = RequestMethod.POST,
      path = "/api/book",
      api = "BookApi.createBook",
      apiDescription = "create a book",
      service = "BookService.createBook",
      servicePostProcessing = true,
      reqRef = "BookCreateRequest",
      resRef = BookApiSpec.DTO_BOOK_DETAIL,
      authenticationDetail = CurrentUser::class,
      deprecated = true,
      deprecatedMessage = "use AuthorApi.createBook instead",
      deprecatedReplacement = ReplaceWith("AuthorApi.createBook"),
    ),
    GenApi(
      method = RequestMethod.POST,
      path = "/api/author/{authorId}/book",
      pathVariable = [
        ApiPathVariable("authorId", Long::class)
      ],
      api = "AuthorApi.createBook",
      apiDescription = "create a book to author",
      service = "AuthorService.createBook",
      servicePostProcessing = true,
      reqRef = "BookCreateRequest",
      resRef = BookApiSpec.DTO_BOOK_DETAIL,
      authenticationDetail = CurrentUser::class,
    )
  ]
)
interface BookApiSpec {
  companion object {
    const val DTO_BOOK = "BookDto"
    const val DTO_BOOK_DETAIL = "BookDetailDto"
    const val REQ_BOOK_CREATE = "BookCreateRequest"
    const val REQ_BOOK_SEARCH = "BookSearchRequest"
  }
}
