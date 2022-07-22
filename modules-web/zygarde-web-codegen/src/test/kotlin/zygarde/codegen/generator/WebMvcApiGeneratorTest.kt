package zygarde.codegen.generator

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.shouldBe
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
}
