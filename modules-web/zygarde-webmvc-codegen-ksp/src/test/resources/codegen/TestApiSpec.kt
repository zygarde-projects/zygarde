package zygarde.test.input

import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.ApiPathVariable
import zygarde.codegen.GenApi
import zygarde.codegen.RequestBodyContentType
import zygarde.codegen.ZyApi

@ZyApi(
  api = [
    GenApi(
      method = RequestMethod.GET,
      path = "/api/test",
      api = "TestApi.listTest",
      apiDescription = "List test items",
      service = "TestService.listTest",
      resRefClass = String::class,
      resCollection = true,
    ),
    GenApi(
      method = RequestMethod.PATCH,
      path = "/api/test/{id}",
      pathVariable = [ApiPathVariable("id", Long::class)],
      api = "TestApi.patchTest",
      apiDescription = "Patch test item",
      service = "TestService.patchTest",
      reqRefClass = String::class,
      resRefClass = String::class,
      requestBodyContentType = RequestBodyContentType.JSON_MERGE_PATCH,
    ),
  ]
)
object TestApiSpec
