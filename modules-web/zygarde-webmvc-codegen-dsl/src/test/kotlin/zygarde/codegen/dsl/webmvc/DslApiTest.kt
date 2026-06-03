package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.RequestBodyContentType
import zygarde.codegen.model.CrudOperationKind
import zygarde.core.exception.ApiErrorCode

class DslApiTest {
  data class TestRequest(val name: String)

  data class TestResponse(val id: Int, val name: String)

  class TestEntity

  interface TestDao

  class TestCreateHook

  class TestPageHook

  private fun createConfig() = WebMvcDslCodegenConfig(
    apiInterfacePackage = "com.test.api",
    controllerPackage = "com.test.controller",
    serviceInterfacePackage = "com.test.service",
    serviceImplPackage = "com.test.service.impl",
  )

  @Test
  fun `should create DslApi with required parameters`() {
    // given
    val config = createConfig()

    // when
    val api = DslApi(config, "TestApi", "/api/test")

    // then
    api.toApiToGenerateVo().apply {
      apiName shouldBe "TestApi"
      basePath shouldBe "/api/test"
    }
  }

  @Test
  fun `should create DslApi without basePath`() {
    // given
    val config = createConfig()

    // when
    val api = DslApi(config, "TestApi")

    // then
    api.toApiToGenerateVo().apply {
      apiName shouldBe "TestApi"
      basePath shouldBe null
    }
  }

  @Test
  fun `should get feignUrlProperty`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi")

    // when
    api.feignUrlProperty = "test.url"

    // then
    api.feignUrlProperty shouldBe "test.url"
  }

  @Test
  fun `should add GET function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.get("getTest", "/test") {
      res<TestResponse>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "getTest"
      path shouldBe "/test"
      method shouldBe RequestMethod.GET
    }
  }

  @Test
  fun `should add POST function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.post("createTest", "/test") {
      req<TestRequest>()
      res<TestResponse>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "createTest"
      method shouldBe RequestMethod.POST
      requestType shouldNotBe null
      responseType shouldNotBe null
    }
  }

  @Test
  fun `should add PUT function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.put("updateTest", "/test/{id}") {
      pathVariable<Int>("id")
      req<TestRequest>()
      res<TestResponse>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "updateTest"
      method shouldBe RequestMethod.PUT
    }
  }

  @Test
  fun `should add DELETE function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.delete("deleteTest", "/test/{id}") {
      pathVariable<Int>("id")
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "deleteTest"
      method shouldBe RequestMethod.DELETE
    }
  }

  @Test
  fun `should add PATCH function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.patch("patchTest", "/test/{id}") {
      pathVariable<Int>("id")
      req<TestRequest>()
      res<TestResponse>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "patchTest"
      method shouldBe RequestMethod.PATCH
      requestBodyContentType shouldBe RequestBodyContentType.DEFAULT
    }
  }

  @Test
  fun `should add JSON merge patch function`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.mergePatch("mergePatchTest", "/test/{id}") {
      pathVariable<Int>("id")
      req<TestRequest>()
      res<TestResponse>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "mergePatchTest"
      method shouldBe RequestMethod.PATCH
      requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
    }
  }

  @Test
  fun `should add GET function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.get<TestRequest, TestResponse>("getWithTypes", "/test") {
      description = "Get with reified types"
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      functionName shouldBe "getWithTypes"
      method shouldBe RequestMethod.GET
      requestType shouldNotBe null
      responseType shouldNotBe null
    }
  }

  @Test
  fun `should add POST function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.post<TestRequest, TestResponse>("postWithTypes", "/test") {
      description = "Post with reified types"
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.POST
      requestType shouldNotBe null
      responseType shouldNotBe null
    }
  }

  @Test
  fun `should add PUT function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.put<TestRequest, TestResponse>("putWithTypes", "/test") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].method shouldBe RequestMethod.PUT
  }

  @Test
  fun `should add PATCH function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.patch<TestRequest, TestResponse>("patchWithTypes", "/test") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].method shouldBe RequestMethod.PATCH
  }

  @Test
  fun `should add JSON merge patch function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.mergePatch<TestRequest, TestResponse>("mergePatchWithTypes", "/test") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.PATCH
      requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
    }
  }

  @Test
  fun `should add GET list function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.getList<TestRequest, TestResponse>("getList", "/test/list") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.GET
      responseTypeGenericArguments shouldHaveSize 1
    }
  }

  @Test
  fun `should add POST list function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.postList<TestRequest, TestResponse>("postList", "/test/list") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.POST
      responseTypeGenericArguments shouldHaveSize 1
    }
  }

  @Test
  fun `should add GET page function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.getPage<TestRequest, TestResponse>("getPage", "/test/page") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.GET
      responseTypeGenericArguments shouldHaveSize 1
    }
  }

  @Test
  fun `should add POST page function with reified types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.postPage<TestRequest, TestResponse>("postPage", "/test/page") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      method shouldBe RequestMethod.POST
      responseTypeGenericArguments shouldHaveSize 1
    }
  }

  @Test
  fun `should add multiple functions`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.get("get1", "/1") {}
    api.post("post1", "/1") {}
    api.put("put1", "/1") {}
    api.delete("delete1", "/1") {}
    api.patch("patch1", "/1") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 5
  }

  @Test
  fun `should set feign URL property`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.feignUrlProperty = "test.api.url"

    // then
    val vo = api.toApiToGenerateVo()
    vo.feignUrlProperty shouldBe "test.api.url"
  }

  @Test
  fun `should convert to ApiToGenerateVo with config values`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service",
      serviceImplPackage = "com.example.service.impl",
    )
    val api = DslApi(config, "ExampleApi", "/api/example")

    // when
    val vo = api.toApiToGenerateVo()

    // then
    vo.apiInterfacePackage shouldBe "com.example.api"
    vo.controllerPackage shouldBe "com.example.controller"
    vo.serviceInterfacePackage shouldBe "com.example.service"
    vo.serviceImplPackage shouldBe "com.example.service.impl"
    vo.apiName shouldBe "ExampleApi"
    vo.basePath shouldBe "/api/example"
    vo.separateFeign shouldBe true
  }

  @Test
  fun `should handle Unit request and response types`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.post<Unit, Unit>("testUnit", "/test") {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].apply {
      requestType shouldBe null
      responseType shouldBe null
    }
  }

  @Test
  fun `should declare CRUD service impl metadata`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.get("getTestList", "") {
      resCollection<TestResponse>()
    }
    api.crudServiceImpl<TestEntity, Int>("TestService") {
      dao<TestDao>("testDao")
      dtoBuilder<TestResponse>("TestResponseBuilder")
      applyExtensions("TestApplyValueExtensions")
      list("getTestList")
    }

    // then
    val crudServiceImpl = api.toApiToGenerateVo().crudServiceImpls.single()
    crudServiceImpl.serviceName shouldBe "TestService"
    crudServiceImpl.daoPropertyName shouldBe "testDao"
    crudServiceImpl.dtoBuilderType.canonicalName shouldBe "zygarde.codegen.model.extensions.TestResponseBuilder"
    crudServiceImpl.applyExtensionsType?.canonicalName shouldBe "zygarde.codegen.model.extensions.TestApplyValueExtensions"
    crudServiceImpl.operations.single().kind shouldBe CrudOperationKind.LIST
    crudServiceImpl.operations.single().functionName shouldBe "getTestList"
  }

  @Test
  fun `should declare enhanced CRUD service impl metadata`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // when
    api.crudServiceImpl<TestEntity, Int>("TestService") {
      dao<TestDao>("testDao")
      dtoBuilder<TestResponse>("TestResponseBuilder")
      transactional("testTx")
      notFound<ApiErrorCode>("NOT_FOUND")
      softDeleteTimestamp("deletedAt")
      page<TestRequest>("searchTests") {
        daoMethod("searchPage")
        hook<TestPageHook>()
        notFound<ApiErrorCode>("BAD_REQUEST")
      }
      create<TestRequest>("createTest") {
        hook<TestCreateHook>()
      }
    }

    // then
    val crudServiceImpl = api.toApiToGenerateVo().crudServiceImpls.single()
    crudServiceImpl.transactional?.transactionManager shouldBe "testTx"
    crudServiceImpl.notFound?.errorCodeName shouldBe "NOT_FOUND"
    crudServiceImpl.softDeleteTimestamp?.fieldName shouldBe "deletedAt"

    val page = crudServiceImpl.operations.first { it.kind == CrudOperationKind.PAGE }
    page.functionName shouldBe "searchTests"
    page.requestType shouldBe TestRequest::class.asTypeName()
    page.daoMethod shouldBe "searchPage"
    page.hookType?.canonicalName shouldBe "zygarde.codegen.dsl.webmvc.DslApiTest.TestPageHook"
    page.notFound?.errorCodeName shouldBe "BAD_REQUEST"

    val create = crudServiceImpl.operations.first { it.kind == CrudOperationKind.CREATE }
    create.hookType?.canonicalName shouldBe "zygarde.codegen.dsl.webmvc.DslApiTest.TestCreateHook"
  }

  @Test
  fun `should fail fast for duplicate CRUD operation hook`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // expect
    shouldThrow<IllegalArgumentException> {
      api.crudServiceImpl<TestEntity, Int>("TestService") {
        dao<TestDao>("testDao")
        dtoBuilder<TestResponse>("TestResponseBuilder")
        create<TestRequest>("createTest") {
          hook<TestCreateHook>()
          hook<TestCreateHook>()
        }
      }
    }
  }

  @Test
  fun `should fail fast for unsupported CRUD operation hook`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/api/test")

    // expect
    shouldThrow<IllegalArgumentException> {
      api.crudServiceImpl<TestEntity, Int>("TestService") {
        dao<TestDao>("testDao")
        dtoBuilder<TestResponse>("TestResponseBuilder")
        list("getTestList") {
          hook<TestCreateHook>()
        }
      }
    }
  }
}
