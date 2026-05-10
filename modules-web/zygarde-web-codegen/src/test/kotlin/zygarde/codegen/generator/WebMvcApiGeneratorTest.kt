package zygarde.codegen.generator

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo

class WebMvcApiGeneratorTest {
  data class CreateTodoReq(val description: String)

  data class TodoDto(val id: Int, val description: String)

  @Test
  fun `test api generate`() {
    val generator = WebMvcApiGenerator(
      listOf(
        ApiToGenerateVo(
          apiInterfacePackage = "com.example.api",
          controllerPackage = "com.example.controller",
          serviceInterfacePackage = "com.example.service",
          apiName = "Todo",
          basePath = "/api/todo",
          functions = mutableListOf(
            ApiFunctionToGenerateVo(
              method = RequestMethod.POST,
              functionName = "createTodo",
              description = "Create todo",
              path = "",
              pathVariables = mapOf(),
              requestName = "req",
              requestType = CreateTodoReq::class.asTypeName(),
              requestTypeGenericArguments = listOf(),
              responseType = Collection::class.asTypeName(),
              responseTypeGenericArguments = listOf(TodoDto::class.asTypeName()),
              serviceName = "TodoService",
              serviceFunctionName = "createTodo",
              postProcessing = true,
              postProcessingParamType = String::class.asTypeName(),
              authenticationDetailName = "auth",
              authenticationDetailType = String::class.asTypeName()
            )
          ),
          separateFeign = true
        )
      )
    )
    val generateApis = generator.generateApis()
    generateApis.also {
      it.apiInterfaces.size shouldBe 1
      it.feignApiInterfaces.size shouldBe 1
      it.controllers.size shouldBe 1
      it.serviceInterfaces.size shouldBe 1

      // it.controllers.forEach { it.writeTo(System.out) }
    }
  }

  @Test
  fun `should generate absolute mapping paths`() {
    val generateApis = WebMvcApiGenerator(
      listOf(
        ApiToGenerateVo(
          apiInterfacePackage = "com.example.api",
          controllerPackage = "com.example.controller",
          serviceInterfacePackage = "com.example.service",
          apiName = "Todo",
          basePath = "api",
          functions = mutableListOf(
            ApiFunctionToGenerateVo(
              method = RequestMethod.GET,
              functionName = "findTodos",
              path = "todo",
              serviceName = "TodoService",
              serviceFunctionName = "findTodos",
            )
          ),
          separateFeign = false
        )
      )
    ).generateApis()

    val apiInterface = generateApis.apiInterfaces.single().toString()
    val controller = generateApis.controllers.single().toString()

    apiInterface shouldContain """@GetMapping(value=["/api/todo"])"""
    controller shouldContain """@GetMapping(value=["/api/todo"])"""
  }

  @Test
  fun `should generate SpringDoc 2 ParameterObject annotation`() {
    val generateApis = WebMvcApiGenerator(
      listOf(
        ApiToGenerateVo(
          apiInterfacePackage = "com.example.api",
          controllerPackage = "com.example.controller",
          serviceInterfacePackage = "com.example.service",
          apiName = "Todo",
          basePath = "/api",
          functions = mutableListOf(
            ApiFunctionToGenerateVo(
              method = RequestMethod.GET,
              functionName = "searchTodos",
              path = "/todo/search",
              requestName = "req",
              requestType = CreateTodoReq::class.asTypeName(),
              serviceName = "TodoService",
              serviceFunctionName = "searchTodos",
            )
          )
        )
      )
    ).generateApis()

    val controller = generateApis.controllers.single().toString()

    controller shouldContain "import org.springdoc.core.annotations.ParameterObject"
    controller shouldContain "@ParameterObject"
    controller shouldContain """@GetMapping(value=["/api/todo/search"])"""
  }
}
