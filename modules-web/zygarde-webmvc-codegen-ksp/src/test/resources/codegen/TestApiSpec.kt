package zygarde.test.input

import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.GenApi
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
  ]
)
object TestApiSpec
