package zygarde.codegen.model

import com.squareup.kotlinpoet.STRING
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.RequestBodyContentType

class ApiFunctionToGenerateVoTest {
  @Test
  fun `should create ApiFunctionToGenerateVo with required fields`() {
    // given & when
    val vo = ApiFunctionToGenerateVo(
      method = RequestMethod.GET,
      functionName = "getUser",
      path = "/users/{id}"
    )

    // then
    vo.method shouldBe RequestMethod.GET
    vo.functionName shouldBe "getUser"
    vo.path shouldBe "/users/{id}"
    vo.description shouldBe ""
    vo.pathVariables shouldBe emptyMap()
    vo.requestName shouldBe "req"
    vo.requestType shouldBe null
    vo.requestTypeGenericArguments shouldBe emptyList()
    vo.requestBodyContentType shouldBe RequestBodyContentType.DEFAULT
    vo.responseType shouldBe null
    vo.responseTypeGenericArguments shouldBe emptyList()
    vo.serviceName shouldBe null
    vo.serviceFunctionName shouldBe null
    vo.postProcessing shouldBe false
    vo.postProcessingParamType shouldBe null
    vo.authenticationDetailName shouldBe "auth"
    vo.authenticationDetailType shouldBe null
    vo.deprecated shouldBe null
  }

  @Test
  fun `should create ApiFunctionToGenerateVo with all fields`() {
    // given
    val pathVars = mapOf("id" to STRING)
    val requestType = STRING
    val responseType = STRING
    val deprecation = Deprecated("Use newMethod instead")

    // when
    val vo = ApiFunctionToGenerateVo(
      method = RequestMethod.POST,
      functionName = "createUser",
      description = "Create a new user",
      path = "/users",
      pathVariables = pathVars,
      requestName = "createReq",
      requestType = requestType,
      requestTypeGenericArguments = listOf(STRING),
      requestBodyContentType = RequestBodyContentType.JSON_MERGE_PATCH,
      responseType = responseType,
      responseTypeGenericArguments = listOf(STRING),
      serviceName = "UserService",
      serviceFunctionName = "create",
      postProcessing = true,
      postProcessingParamType = STRING,
      authenticationDetailName = "userAuth",
      authenticationDetailType = STRING,
      deprecated = deprecation
    )

    // then
    vo.method shouldBe RequestMethod.POST
    vo.functionName shouldBe "createUser"
    vo.description shouldBe "Create a new user"
    vo.path shouldBe "/users"
    vo.pathVariables shouldBe pathVars
    vo.requestName shouldBe "createReq"
    vo.requestType shouldBe requestType
    vo.requestTypeGenericArguments shouldBe listOf(STRING)
    vo.requestBodyContentType shouldBe RequestBodyContentType.JSON_MERGE_PATCH
    vo.responseType shouldBe responseType
    vo.responseTypeGenericArguments shouldBe listOf(STRING)
    vo.serviceName shouldBe "UserService"
    vo.serviceFunctionName shouldBe "create"
    vo.postProcessing shouldBe true
    vo.postProcessingParamType shouldBe STRING
    vo.authenticationDetailName shouldBe "userAuth"
    vo.authenticationDetailType shouldBe STRING
    vo.deprecated shouldBe deprecation
  }

  @Test
  fun `should support different HTTP methods`() {
    val methods = listOf(
      RequestMethod.GET,
      RequestMethod.POST,
      RequestMethod.PUT,
      RequestMethod.DELETE,
      RequestMethod.PATCH
    )

    methods.forEach { method ->
      val vo = ApiFunctionToGenerateVo(
        method = method,
        functionName = "test",
        path = "/test"
      )
      vo.method shouldBe method
    }
  }
}
