package zygarde.codegen.dsl.webmvc

import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WebMvcDslCodegenTest : WebMvcDslCodegen() {
  data class CreateTodoReq(val description: String)

  data class UpdateTodoReq(val description: String)

  data class TodoDto(val id: Int, val description: String)

  data class GetTodoByIdReq(val id: Int)

  override fun codegen() {
    api("TodoApi", "/api/todo") {
      fun DslApiFunction.todoIdPathVariable() {
        pathVariable<Int>("todoId")
      }

      get("getTodoById", "byId") {
        req<GetTodoByIdReq>("req")
        resCollection<TodoDto>()
      }
      get("getTodoList", "") {
        resCollection<TodoDto>()
      }
      get("getTodo", "{todoId}") {
        todoIdPathVariable()
        res<TodoDto>()
      }
      post("createTodo", "") {
        req<CreateTodoReq>()
        res<TodoDto>()
      }
      put("updateTodo", "{todoId}") {
        todoIdPathVariable()
        req<UpdateTodoReq>()
        res<TodoDto>()
      }
      delete("deleteTodo", "{todoId}") {
        todoIdPathVariable()
      }
    }

    // Test the 2-parameter api method (without basePath)
    api("SimpleApi") {
      get("getSimple", "/simple") {
        res<TodoDto>()
      }
    }
  }

  @Test
  fun `test webmvc dsl codegen`() {
    main()
  }

  @Test
  fun `test webmvc dsl codegen with file output`(
    @TempDir tempDir: Path
  ) {
    // given - set system properties to write to temp directory
    System.setProperty("zygarde.codegen.dsl.webmvc.api-interface.write-to", tempDir.resolve("api").toString())
    System.setProperty("zygarde.codegen.dsl.webmvc.feign-interface.write-to", tempDir.resolve("feign").toString())
    System.setProperty("zygarde.codegen.dsl.webmvc.controller.write-to", tempDir.resolve("controller").toString())
    System.setProperty("zygarde.codegen.dsl.webmvc.service-interface.write-to", tempDir.resolve("service").toString())

    try {
      // when
      main()

      // then - verify directories were created
      tempDir.resolve("api").toFile().shouldExist()
      tempDir.resolve("feign").toFile().shouldExist()
      tempDir.resolve("controller").toFile().shouldExist()
      tempDir.resolve("service").toFile().shouldExist()
    } finally {
      // cleanup - clear system properties
      System.clearProperty("zygarde.codegen.dsl.webmvc.api-interface.write-to")
      System.clearProperty("zygarde.codegen.dsl.webmvc.feign-interface.write-to")
      System.clearProperty("zygarde.codegen.dsl.webmvc.controller.write-to")
      System.clearProperty("zygarde.codegen.dsl.webmvc.service-interface.write-to")
    }
  }

  @Test
  fun `should generate apis from codegen`() {
    // given
    codegen()

    // when
    val apis = apisToGenerate

    // then
    apis.size shouldBe 2
    apis[0].apiName shouldBe "TodoApi"
    apis[1].apiName shouldBe "SimpleApi"
  }
}
