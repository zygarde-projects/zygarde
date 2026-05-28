package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.RequestBodyContentType
import zygarde.data.api.PageDto

class DslApiFunctionTest {
  data class TestRequest(val name: String)

  data class TestResponse(val id: Int, val name: String)

  data class TestAuth(val userId: String)

  @Test
  fun `should create DslApiFunction with required parameters`() {
    // when
    val function = DslApiFunction("testFunction", "/test", RequestMethod.GET)

    // then
    function.functionName shouldBe "testFunction"
    function.path shouldBe "/test"
    function.method shouldBe RequestMethod.GET
  }

  @Test
  fun `should set default values`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // then
    function.description shouldBe ""
    function.requestName shouldBe "req"
    function.serviceName shouldBe null
    function.serviceFunctionName shouldBe null
    function.requestType shouldBe null
    function.responseType shouldBe null
    function.requestBodyContentType shouldBe RequestBodyContentType.DEFAULT
    function.servicePostProcessing shouldBe false
    function.authenticationDetailName shouldBe "auth"
  }

  @Test
  fun `should add path variable by KClass`() {
    // given
    val function = DslApiFunction("test", "/test/{id}", RequestMethod.GET)

    // when
    function.pathVariable("id", Int::class)

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.pathVariables shouldContainKey "id"
    vo.pathVariables["id"] shouldBe Int::class.asTypeName()
  }

  @Test
  fun `should add path variable by TypeName`() {
    // given
    val function = DslApiFunction("test", "/test/{id}", RequestMethod.GET)

    // when
    function.pathVariable("id", String::class.asTypeName())

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.pathVariables shouldContainKey "id"
  }

  @Test
  fun `should add path variable with reified type`() {
    // given
    val function = DslApiFunction("test", "/test/{id}", RequestMethod.GET)

    // when
    function.pathVariable<Long>("id")

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.pathVariables shouldContainKey "id"
    vo.pathVariables["id"] shouldBe Long::class.asTypeName()
  }

  @Test
  fun `should add multiple path variables`() {
    // given
    val function = DslApiFunction("test", "/test/{id}/{name}", RequestMethod.GET)

    // when
    function.pathVariables("id" to Int::class, "name" to String::class)

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.pathVariables.keys shouldHaveSize 2
    vo.pathVariables shouldContainKey "id"
    vo.pathVariables shouldContainKey "name"
  }

  @Test
  fun `should set request type with reified`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST)

    // when
    function.req<TestRequest>("request")

    // then
    function.requestName shouldBe "request"
    function.requestType shouldBe TestRequest::class.asTypeName()
  }

  @Test
  fun `should set request type with default name`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST)

    // when
    function.req<TestRequest>()

    // then
    function.requestName shouldBe "req"
    function.requestType shouldBe TestRequest::class.asTypeName()
  }

  @Test
  fun `should set request collection type with reified`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST)

    // when
    function.reqCollection<TestRequest>("requests")

    // then
    function.requestName shouldBe "requests"
    function.requestType shouldBe Collection::class.asTypeName()
    function.requestTypeGenericArguments shouldContainExactly listOf(TestRequest::class.asTypeName())
  }

  @Test
  fun `should set request collection type with default name`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST)

    // when
    function.reqCollection<TestRequest>()

    // then
    function.requestName shouldBe "reqList"
    function.requestType shouldBe Collection::class.asTypeName()
  }

  @Test
  fun `should set JSON merge patch request content type`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.PATCH)

    // when
    function.jsonMergePatch()

    // then
    function.requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
    function.toApiFunctionToGenerate().requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
  }

  @Test
  fun `should set response type with reified`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // when
    function.res<TestResponse>()

    // then
    function.responseType shouldBe TestResponse::class.asTypeName()
  }

  @Test
  fun `should set response collection type with reified`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // when
    function.resCollection<TestResponse>()

    // then
    function.responseType shouldBe Collection::class.asTypeName()
    function.responseTypeGenericArguments shouldContainExactly listOf(TestResponse::class.asTypeName())
  }

  @Test
  fun `should set response page type with reified`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // when
    function.resPage<TestResponse>()

    // then
    function.responseType shouldBe PageDto::class.asTypeName()
    function.responseTypeGenericArguments shouldContainExactly listOf(TestResponse::class.asTypeName())
  }

  @Test
  fun `should set authentication detail type`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // when
    function.auth<TestAuth>("authentication")

    // then
    function.authenticationDetailName shouldBe "authentication"
    function.authenticationDetailType shouldBe TestAuth::class.asTypeName()
  }

  @Test
  fun `should set authentication detail type with default name`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.GET)

    // when
    function.auth<TestAuth>()

    // then
    function.authenticationDetailName shouldBe "auth"
    function.authenticationDetailType shouldBe TestAuth::class.asTypeName()
  }

  @Test
  fun `should set service post processing`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST)

    // when
    function.servicePostProcessing<TestRequest>()

    // then
    function.servicePostProcessing shouldBe true
    function.servicePostProcessingExtraParamType shouldBe TestRequest::class.asTypeName()
  }

  @Test
  fun `should convert to ApiFunctionToGenerateVo`() {
    // given
    val function = DslApiFunction("testFunc", "/test/{id}", RequestMethod.POST).apply {
      description = "Test function"
      pathVariable<Int>("id")
      req<TestRequest>()
      res<TestResponse>()
      jsonMergePatch()
      serviceName = "TestService"
      serviceFunctionName = "testServiceFunc"
    }

    // when
    val vo = function.toApiFunctionToGenerate()

    // then
    vo.functionName shouldBe "testFunc"
    vo.path shouldBe "/test/{id}"
    vo.method shouldBe RequestMethod.POST
    vo.description shouldBe "Test function"
    vo.pathVariables shouldContainKey "id"
    vo.requestType shouldBe TestRequest::class.asTypeName()
    vo.requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
    vo.responseType shouldBe TestResponse::class.asTypeName()
    vo.serviceName shouldBe "TestService"
    vo.serviceFunctionName shouldBe "testServiceFunc"
  }

  @Test
  fun `should use functionName as serviceFunctionName when not set`() {
    // given
    val function = DslApiFunction("myFunction", "/test", RequestMethod.GET)

    // when
    val vo = function.toApiFunctionToGenerate()

    // then
    vo.serviceFunctionName shouldBe "myFunction"
  }

  @Test
  fun `should set postProcessing when servicePostProcessingExtraParamType is set`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST).apply {
      servicePostProcessing<TestRequest>()
    }

    // when
    val vo = function.toApiFunctionToGenerate()

    // then
    vo.postProcessing shouldBe true
    vo.postProcessingParamType shouldNotBe null
  }

  @Test
  fun `should set postProcessing when servicePostProcessing flag is true`() {
    // given
    val function = DslApiFunction("test", "/test", RequestMethod.POST).apply {
      servicePostProcessing = true
    }

    // when
    val vo = function.toApiFunctionToGenerate()

    // then
    vo.postProcessing shouldBe true
  }
}
