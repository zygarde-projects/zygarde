package codegen.spec

import org.springframework.web.bind.annotation.RequestMethod
import codegen.spec.AuthorApiSpec.Companion.API_AUTHOR
import codegen.spec.AuthorApiSpec.Companion.DTO_AUTHOR
import codegen.spec.AuthorApiSpec.Companion.DTO_AUTHOR_DETAIL
import codegen.spec.AuthorApiSpec.Companion.SERVICE_AUTHOR
import zygarde.codegen.ApiPathVariable
import zygarde.codegen.GenApi
import zygarde.codegen.ZyApi

@ZyApi(
  [
    GenApi(
      method = RequestMethod.GET,
      path = "/api/author",
      pathVariable = [],
      api = "${API_AUTHOR}.getAuthors",
      apiDescription = "get all authors",
      service = "${SERVICE_AUTHOR}.getAllAuthors",
      reqRef = "",
      resRef = DTO_AUTHOR,
      resCollection = true
    ),
    GenApi(
      method = RequestMethod.GET,
      path = "/api/author/{authorId}",
      pathVariable = [
        ApiPathVariable("authorId", Long::class)
      ],
      api = "${API_AUTHOR}.getAuthor",
      apiDescription = "get author detail",
      service = "${SERVICE_AUTHOR}.getAuthorDetail",
      reqRef = "",
      resRef = DTO_AUTHOR_DETAIL,
      resCollection = false
    )
  ]
)
interface AuthorApiSpec {
  companion object {
    const val API_AUTHOR = "AuthorApi"
    const val SERVICE_AUTHOR = "AuthorService"
    const val DTO_AUTHOR = "AuthorDto"
    const val DTO_AUTHOR_DETAIL = "AuthorDetailDto"
  }
}
