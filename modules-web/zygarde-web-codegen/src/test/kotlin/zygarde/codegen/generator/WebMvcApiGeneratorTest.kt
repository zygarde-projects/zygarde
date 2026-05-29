package zygarde.codegen.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.RequestBodyContentType
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.codegen.model.CrudOperationKind
import zygarde.codegen.model.CrudOperationToGenerateVo
import zygarde.codegen.model.CrudServiceImplToGenerateVo

class WebMvcApiGeneratorTest {
  data class CreateTodoReq(val description: String)

  data class UpdateTodoReq(val description: String)

  data class TodoPatchReq(val description: String?)

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
      it.serviceImpls.size shouldBe 0

      // it.controllers.forEach { it.writeTo(System.out) }
    }
  }

  @Test
  fun `should generate CRUD service impl`() {
    val generateApis = WebMvcApiGenerator(
      listOf(crudApi())
    ).generateApis()

    val serviceImpl = generateApis.serviceImpls.single().toString()

    serviceImpl shouldContain "package com.example.service.`impl`"
    serviceImpl shouldContain "import com.example.extensions.TodoApplyValueExtensions.applyFrom"
    serviceImpl shouldContain "import com.example.extensions.TodoPatchExtensions.applyPatch"
    serviceImpl shouldContain "import org.springframework.stereotype.Service"
    serviceImpl shouldContain "@Service"
    serviceImpl shouldContain "public class TodoServiceImpl"
    serviceImpl shouldContain ": TodoService"
    serviceImpl shouldContain "@Autowired"
    serviceImpl shouldContain "private val todoDao: TodoDao"
    serviceImpl shouldContain "todoDao.findAll().map(TodoDtoBuilder::build)"
    serviceImpl shouldContain "todoDao.getById(todoId).let(TodoDtoBuilder::build)"
    serviceImpl shouldContain "Todo().applyFrom(req).let(todoDao::saveAndFlush).let(TodoDtoBuilder::build)"
    serviceImpl shouldContain "todoDao.getById(todoId).applyFrom(req).let(todoDao::saveAndFlush).let(TodoDtoBuilder::build)"
    serviceImpl shouldContain "todoDao.deleteById(todoId)"
    serviceImpl shouldContain "todoDao.getById(todoId).applyPatch(req).let(todoDao::saveAndFlush).let(TodoDtoBuilder::build)"
  }

  @Test
  fun `should fail when CRUD function does not exist`() {
    shouldThrow<IllegalStateException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            operations = listOf(CrudOperationToGenerateVo(CrudOperationKind.LIST, "missing"))
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD idParam is not a path variable`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            operations = listOf(CrudOperationToGenerateVo(CrudOperationKind.GET, "getTodo", "missingId"))
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD request DTO is missing`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            functions = mutableListOf(
              ApiFunctionToGenerateVo(
                method = RequestMethod.POST,
                functionName = "createTodo",
                path = "",
                responseType = TodoDto::class.asTypeName(),
                serviceName = "TodoService",
              )
            ),
            operations = listOf(
              CrudOperationToGenerateVo(CrudOperationKind.CREATE, "createTodo", requestType = CreateTodoReq::class.asTypeName())
            )
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD delete declares response DTO`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            functions = mutableListOf(
              ApiFunctionToGenerateVo(
                method = RequestMethod.DELETE,
                functionName = "deleteTodo",
                path = "{todoId}",
                pathVariables = mapOf("todoId" to Int::class.asTypeName()),
                responseType = TodoDto::class.asTypeName(),
                serviceName = "TodoService",
              )
            ),
            operations = listOf(CrudOperationToGenerateVo(CrudOperationKind.DELETE, "deleteTodo", "todoId"))
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD function uses post processing`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            functions = mutableListOf(
              ApiFunctionToGenerateVo(
                method = RequestMethod.GET,
                functionName = "getTodoList",
                path = "",
                responseType = Collection::class.asTypeName(),
                responseTypeGenericArguments = listOf(TodoDto::class.asTypeName()),
                serviceName = "TodoService",
                postProcessing = true,
              )
            ),
            operations = listOf(CrudOperationToGenerateVo(CrudOperationKind.LIST, "getTodoList"))
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD function uses authentication detail`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(
          crudApi(
            functions = mutableListOf(
              ApiFunctionToGenerateVo(
                method = RequestMethod.GET,
                functionName = "getTodoList",
                path = "",
                responseType = Collection::class.asTypeName(),
                responseTypeGenericArguments = listOf(TodoDto::class.asTypeName()),
                serviceName = "TodoService",
                authenticationDetailType = String::class.asTypeName(),
              )
            ),
            operations = listOf(CrudOperationToGenerateVo(CrudOperationKind.LIST, "getTodoList"))
          )
        )
      ).generateApis()
    }
  }

  @Test
  fun `should fail when CRUD service impl is declared twice for same service`() {
    shouldThrow<IllegalArgumentException> {
      WebMvcApiGenerator(
        listOf(crudApi(), crudApi(apiName = "TodoApi2"))
      ).generateApis()
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

  @Test
  fun `should generate request parameters`() {
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
              requestParams = mapOf(
                "keyword" to String::class.asTypeName().copy(nullable = true),
                "pageSize" to Int::class.asTypeName(),
              ),
              responseType = Collection::class.asTypeName(),
              responseTypeGenericArguments = listOf(TodoDto::class.asTypeName()),
              serviceName = "TodoService",
              serviceFunctionName = "searchTodos",
            )
          )
        )
      )
    ).generateApis()

    val feignApiInterface = generateApis.feignApiInterfaces.single().toString()
    val controller = generateApis.controllers.single().toString()
    val serviceInterface = generateApis.serviceInterfaces.single().toString()

    feignApiInterface shouldContain "@RequestParam(value=\"keyword\", required=false)"
    feignApiInterface shouldContain "@RequestParam(value=\"pageSize\")"
    controller shouldContain "keyword: String?"
    controller shouldContain "pageSize: Int"
    serviceInterface shouldContain "public fun searchTodos(keyword: String?, pageSize: Int):"
  }

  @Test
  fun `should generate JSON merge patch mapping`() {
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
              method = RequestMethod.PATCH,
              functionName = "patchTodo",
              path = "/todo/{id}",
              pathVariables = mapOf("id" to Int::class.asTypeName()),
              requestName = "patch",
              requestType = CreateTodoReq::class.asTypeName(),
              requestBodyContentType = RequestBodyContentType.JSON_MERGE_PATCH,
              responseType = TodoDto::class.asTypeName(),
              serviceName = "TodoService",
              serviceFunctionName = "patchTodo",
            )
          )
        )
      )
    ).generateApis()

    val feignApiInterface = generateApis.feignApiInterfaces.single().toString()
    val controller = generateApis.controllers.single().toString()

    feignApiInterface shouldContain "import org.springframework.web.bind.`annotation`.PatchMapping"
    feignApiInterface shouldContain "@PatchMapping("
    feignApiInterface shouldContain """value=["/api/todo/{id}"]"""
    feignApiInterface shouldContain """consumes=["application/merge-patch+json"]"""
    feignApiInterface shouldContain "@RequestBody"
    controller shouldContain "@PatchMapping("
    controller shouldContain """value=["/api/todo/{id}"]"""
    controller shouldContain """consumes=["application/merge-patch+json"]"""
    controller shouldContain "@RequestBody"
  }

  @Test
  fun `should generate response status on controller only`() {
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
              method = RequestMethod.POST,
              functionName = "createTodo",
              path = "/todo",
              responseStatus = HttpStatus.CREATED,
              serviceName = "TodoService",
              serviceFunctionName = "createTodo",
            )
          )
        )
      )
    ).generateApis()

    val apiInterface = generateApis.apiInterfaces.single().toString()
    val feignApiInterface = generateApis.feignApiInterfaces.single().toString()
    val controller = generateApis.controllers.single().toString()

    apiInterface shouldNotContain "@ResponseStatus"
    apiInterface shouldNotContain "@ApiResponse"
    feignApiInterface shouldNotContain "@ResponseStatus"
    feignApiInterface shouldNotContain "@ApiResponse"
    controller shouldContain "import io.swagger.v3.oas.annotations.responses.ApiResponse"
    controller shouldContain "@ApiResponse(responseCode = \"201\")"
    controller shouldContain "import org.springframework.web.bind.`annotation`.ResponseStatus"
    controller shouldContain "@ResponseStatus(HttpStatus.CREATED)"
  }

  private fun crudApi(
    apiName: String = "TodoApi",
    functions: MutableList<ApiFunctionToGenerateVo> = crudFunctions(),
    operations: List<CrudOperationToGenerateVo> = crudOperations(),
  ): ApiToGenerateVo {
    return ApiToGenerateVo(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service",
      apiName = apiName,
      basePath = "/api/todo",
      functions = functions,
      serviceImplPackage = "com.example.service.impl",
      crudServiceImpls = listOf(
        CrudServiceImplToGenerateVo(
          serviceName = "TodoService",
          entityType = ClassName("com.example.model", "Todo"),
          idType = Int::class.asTypeName(),
          daoType = ClassName("com.example.dao", "TodoDao"),
          daoPropertyName = "todoDao",
          dtoBuilderType = ClassName("com.example.extensions", "TodoDtoBuilder"),
          applyExtensionsType = ClassName("com.example.extensions", "TodoApplyValueExtensions"),
          patchExtensionsType = ClassName("com.example.extensions", "TodoPatchExtensions"),
          operations = operations,
        )
      )
    )
  }

  private fun crudFunctions(): MutableList<ApiFunctionToGenerateVo> {
    return mutableListOf(
      ApiFunctionToGenerateVo(
        method = RequestMethod.GET,
        functionName = "getTodoList",
        path = "",
        responseType = Collection::class.asTypeName(),
        responseTypeGenericArguments = listOf(TodoDto::class.asTypeName()),
        serviceName = "TodoService",
      ),
      ApiFunctionToGenerateVo(
        method = RequestMethod.GET,
        functionName = "getTodo",
        path = "{todoId}",
        pathVariables = mapOf("todoId" to Int::class.asTypeName()),
        responseType = TodoDto::class.asTypeName(),
        serviceName = "TodoService",
      ),
      ApiFunctionToGenerateVo(
        method = RequestMethod.POST,
        functionName = "createTodo",
        path = "",
        requestType = CreateTodoReq::class.asTypeName(),
        responseType = TodoDto::class.asTypeName(),
        serviceName = "TodoService",
      ),
      ApiFunctionToGenerateVo(
        method = RequestMethod.PUT,
        functionName = "updateTodo",
        path = "{todoId}",
        pathVariables = mapOf("todoId" to Int::class.asTypeName()),
        requestType = UpdateTodoReq::class.asTypeName(),
        responseType = TodoDto::class.asTypeName(),
        serviceName = "TodoService",
      ),
      ApiFunctionToGenerateVo(
        method = RequestMethod.DELETE,
        functionName = "deleteTodo",
        path = "{todoId}",
        pathVariables = mapOf("todoId" to Int::class.asTypeName()),
        serviceName = "TodoService",
      ),
      ApiFunctionToGenerateVo(
        method = RequestMethod.PATCH,
        functionName = "patchTodo",
        path = "{todoId}",
        pathVariables = mapOf("todoId" to Int::class.asTypeName()),
        requestType = TodoPatchReq::class.asTypeName(),
        responseType = TodoDto::class.asTypeName(),
        serviceName = "TodoService",
      ),
    )
  }

  private fun crudOperations(): List<CrudOperationToGenerateVo> {
    return listOf(
      CrudOperationToGenerateVo(CrudOperationKind.LIST, "getTodoList"),
      CrudOperationToGenerateVo(CrudOperationKind.GET, "getTodo", "todoId"),
      CrudOperationToGenerateVo(CrudOperationKind.CREATE, "createTodo", requestType = CreateTodoReq::class.asTypeName()),
      CrudOperationToGenerateVo(CrudOperationKind.UPDATE, "updateTodo", "todoId", UpdateTodoReq::class.asTypeName()),
      CrudOperationToGenerateVo(CrudOperationKind.DELETE, "deleteTodo", "todoId"),
      CrudOperationToGenerateVo(CrudOperationKind.MERGE_PATCH, "patchTodo", "todoId", TodoPatchReq::class.asTypeName()),
    )
  }
}
