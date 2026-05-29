package zygarde.codegen.model

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod

class ApiToGenerateVoTest {
  @Test
  fun `should create ApiToGenerateVo with required fields`() {
    // given & when
    val vo = ApiToGenerateVo(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service",
      apiName = "User"
    )

    // then
    vo.apiInterfacePackage shouldBe "com.example.api"
    vo.controllerPackage shouldBe "com.example.controller"
    vo.serviceInterfacePackage shouldBe "com.example.service"
    vo.apiName shouldBe "User"
    vo.basePath shouldBe null
    vo.functions.shouldBeEmpty()
    vo.separateFeign shouldBe true
    vo.feignUrlProperty shouldBe null
    vo.serviceImplPackage shouldBe "zygarde.generated.service.impl"
    vo.crudServiceImpls.shouldBeEmpty()
  }

  @Test
  fun `should create ApiToGenerateVo with all fields`() {
    // given
    val function = ApiFunctionToGenerateVo(
      method = RequestMethod.GET,
      functionName = "getUser",
      path = "/users/{id}"
    )

    // when
    val vo = ApiToGenerateVo(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service",
      apiName = "User",
      basePath = "/api/v1",
      functions = mutableListOf(function),
      separateFeign = false,
      feignUrlProperty = "\${api.user.url}"
    )

    // then
    vo.apiInterfacePackage shouldBe "com.example.api"
    vo.controllerPackage shouldBe "com.example.controller"
    vo.serviceInterfacePackage shouldBe "com.example.service"
    vo.apiName shouldBe "User"
    vo.basePath shouldBe "/api/v1"
    vo.functions shouldHaveSize 1
    vo.functions[0] shouldBe function
    vo.separateFeign shouldBe false
    vo.feignUrlProperty shouldBe "\${api.user.url}"
  }

  @Test
  fun `should support adding functions to mutable list`() {
    // given
    val vo = ApiToGenerateVo(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service",
      apiName = "User"
    )

    // when
    vo.functions.add(
      ApiFunctionToGenerateVo(
        method = RequestMethod.GET,
        functionName = "getUser",
        path = "/users/{id}"
      )
    )
    vo.functions.add(
      ApiFunctionToGenerateVo(
        method = RequestMethod.POST,
        functionName = "createUser",
        path = "/users"
      )
    )

    // then
    vo.functions shouldHaveSize 2
    vo.functions[0].functionName shouldBe "getUser"
    vo.functions[1].functionName shouldBe "createUser"
  }
}
