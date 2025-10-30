package zygarde.codegen.dsl.webmvc

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/**
 * Additional tests to improve coverage by exercising various DSL combinations
 */
class DslIntegrationTest {

  data class UserRequest(val name: String, val email: String)
  data class UserResponse(val id: Long, val name: String)
  data class AuthDetails(val userId: String, val roles: List<String>)

  @Test
  fun `should build complete API with all HTTP methods`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.test.api",
      controllerPackage = "com.test.controller",
      serviceInterfacePackage = "com.test.service"
    )
    val api = DslApi(config, "UserApi", "/api/users")

    // when - use all DSL features
    api.get("listUsers", "") {
      resPage<UserResponse>()
      auth<AuthDetails>()
    }

    api.post("createUser", "") {
      req<UserRequest>()
      res<UserResponse>()
      auth<AuthDetails>()
      servicePostProcessing<UserRequest>()
    }

    api.put("updateUser", "/{userId}") {
      pathVariable<Long>("userId")
      req<UserRequest>()
      res<UserResponse>()
    }

    api.delete("deleteUser", "/{userId}") {
      pathVariable<Long>("userId")
      auth<AuthDetails>()
    }

    api.getList<Unit, UserResponse>("searchUsers", "/search") {
      description = "Search users"
    }

    api.postList<UserRequest, UserResponse>("batchCreate", "/batch") {
      description = "Batch create users"
    }

    api.getPage<Unit, UserResponse>("pagedUsers", "/paged") {
      description = "Get paged users"
    }

    api.postPage<UserRequest, UserResponse>("searchPaged", "/search/paged") {
      description = "Search with pagination"
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions.size shouldBe 8
    vo.apiName shouldBe "UserApi"
    vo.basePath shouldBe "/api/users"
  }

  @Test
  fun `should build API without base path`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.test.api",
      controllerPackage = "com.test.controller",
      serviceInterfacePackage = "com.test.service"
    )

    // when
    val api = DslApi(config, "RootApi")
    api.get("health", "/health") {
      res<String>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.basePath shouldBe null
    vo.functions.size shouldBe 1
  }

  @Test
  fun `should build function with multiple path variables`() {
    // given
    val function = DslApiFunction("test", "/users/{userId}/posts/{postId}", org.springframework.web.bind.annotation.RequestMethod.GET)

    // when
    function.pathVariables("userId" to Long::class, "postId" to Int::class)
    function.res<UserResponse>()

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.pathVariables.size shouldBe 2
    vo.pathVariables.containsKey("userId") shouldBe true
    vo.pathVariables.containsKey("postId") shouldBe true
  }

  @Test
  fun `should set service details`() {
    // given
    val function = DslApiFunction("getUser", "/users/{id}", org.springframework.web.bind.annotation.RequestMethod.GET)

    // when
    function.pathVariable<Long>("id")
    function.res<UserResponse>()
    function.serviceName = "UserService"
    function.serviceFunctionName = "findUserById"
    function.description = "Get user by ID"

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.serviceName shouldBe "UserService"
    vo.serviceFunctionName shouldBe "findUserById"
    vo.description shouldBe "Get user by ID"
  }

  @Test
  fun `should handle service post processing flag without extra param`() {
    // given
    val function = DslApiFunction("test", "/test", org.springframework.web.bind.annotation.RequestMethod.POST)

    // when
    function.servicePostProcessing = true

    // then
    val vo = function.toApiFunctionToGenerate()
    vo.postProcessing shouldBe true
    vo.postProcessingParamType shouldBe null
  }

  @Test
  fun `should build PUT function with reified types`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.test.api",
      controllerPackage = "com.test.controller",
      serviceInterfacePackage = "com.test.service"
    )
    val api = DslApi(config, "TestApi")

    // when
    api.put<UserRequest, UserResponse>("updateUser", "/users/{id}") {
      pathVariable<Long>("id")
      description = "Update user"
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.functions.size shouldBe 1
    vo.functions[0].method shouldBe org.springframework.web.bind.annotation.RequestMethod.PUT
    vo.functions[0].requestType shouldNotBe null
    vo.functions[0].responseType shouldNotBe null
  }

  @Test
  fun `should configure feign URL`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.test.api",
      controllerPackage = "com.test.controller",
      serviceInterfacePackage = "com.test.service"
    )
    val api = DslApi(config, "ExternalApi")

    // when
    api.feignUrlProperty = "external.service.url"
    api.get("getData", "/data") {
      res<String>()
    }

    // then
    val vo = api.toApiToGenerateVo()
    vo.feignUrlProperty shouldBe "external.service.url"
    vo.separateFeign shouldBe true
  }
}
