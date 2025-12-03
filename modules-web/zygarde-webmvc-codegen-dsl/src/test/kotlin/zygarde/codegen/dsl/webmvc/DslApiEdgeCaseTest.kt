package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod

class DslApiEdgeCaseTest {
  data class TestRequest(val name: String)

  data class TestResponse(val id: Int, val name: String)

  private fun createConfig() = WebMvcDslCodegenConfig(
    apiInterfacePackage = "com.test.api",
    controllerPackage = "com.test.controller",
    serviceInterfacePackage = "com.test.service"
  )

  @Test
  fun `should handle null basePath`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", null)

    // when
    val vo = api.toApiToGenerateVo()

    // then
    vo.basePath.shouldBeNull()
  }

  @Test
  fun `should handle empty functions list`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    val vo = api.toApiToGenerateVo()

    // then
    vo.functions shouldHaveSize 0
  }

  @Test
  fun `should handle null feignUrlProperty`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    val vo = api.toApiToGenerateVo()

    // then
    vo.feignUrlProperty.shouldBeNull()
  }

  @Test
  fun `buildForMethod should create function and add to list`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethod("testFunc", "/path", RequestMethod.GET) {
      description = "Test description"
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].functionName shouldBe "testFunc"
    vo.functions[0].description shouldBe "Test description"
  }

  @Test
  fun `buildForMethodReified should handle Unit request type`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.GET,
      reqClass = Unit::class,
      reqClassGenericClasses = emptyList(),
      resClass = TestResponse::class,
      resClassGenericClasses = emptyList()
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].requestType.shouldBeNull()
    vo.functions[0].responseType.shouldNotBeNull()
  }

  @Test
  fun `buildForMethodReified should handle Unit response type`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.POST,
      reqClass = TestRequest::class,
      reqClassGenericClasses = emptyList(),
      resClass = Unit::class,
      resClassGenericClasses = emptyList()
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].requestType.shouldNotBeNull()
    vo.functions[0].responseType.shouldBeNull()
  }

  @Test
  fun `buildForMethodReified should handle null request class`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.GET,
      reqClass = null,
      reqClassGenericClasses = emptyList(),
      resClass = TestResponse::class,
      resClassGenericClasses = emptyList()
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].requestType.shouldBeNull()
  }

  @Test
  fun `buildForMethodReified should handle null response class`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.DELETE,
      reqClass = null,
      reqClassGenericClasses = emptyList(),
      resClass = null,
      resClassGenericClasses = emptyList()
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].requestType.shouldBeNull()
    vo.functions[0].responseType.shouldBeNull()
  }

  @Test
  fun `buildForMethodReified should handle generic request type`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.POST,
      reqClass = Collection::class,
      reqClassGenericClasses = listOf(TestRequest::class),
      resClass = TestResponse::class,
      resClassGenericClasses = emptyList()
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].requestType shouldBe Collection::class.asTypeName()
    vo.functions[0].requestTypeGenericArguments shouldHaveSize 1
  }

  @Test
  fun `buildForMethodReified should handle generic response type`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "TestApi", "/test")

    // when
    api.buildForMethodReified(
      functionName = "test",
      path = "/path",
      method = RequestMethod.GET,
      reqClass = TestRequest::class,
      reqClassGenericClasses = emptyList(),
      resClass = Collection::class,
      resClassGenericClasses = listOf(TestResponse::class)
    ) {}

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions shouldHaveSize 1
    vo.functions[0].responseType shouldBe Collection::class.asTypeName()
    vo.functions[0].responseTypeGenericArguments shouldHaveSize 1
  }

  @Test
  fun `should set all fields in toApiToGenerateVo`() {
    // given
    val config = createConfig()
    val api = DslApi(config, "MyApi", "/my/api").apply {
      feignUrlProperty = "my.service.url"
      get("test", "/test") {}
    }

    // when
    val vo = api.toApiToGenerateVo()

    // then
    vo.apiInterfacePackage shouldBe "com.test.api"
    vo.controllerPackage shouldBe "com.test.controller"
    vo.serviceInterfacePackage shouldBe "com.test.service"
    vo.apiName shouldBe "MyApi"
    vo.basePath shouldBe "/my/api"
    vo.separateFeign shouldBe true
    vo.feignUrlProperty shouldBe "my.service.url"
    vo.functions shouldHaveSize 1
  }
}
