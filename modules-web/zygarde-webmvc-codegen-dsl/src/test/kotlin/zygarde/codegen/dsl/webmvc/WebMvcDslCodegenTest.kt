package zygarde.codegen.dsl.webmvc

import org.junit.jupiter.api.Test

class WebMvcDslCodegenTest : WebMvcDslCodegen() {

  data class CreateTodoReq(val description: String)
  data class UpdateTodoReq(val description: String)
  data class TodoDto(val id: Int, val description: String)

  override fun codegen() {
    api("TodoApi", "/api/todo") {
      fun DslApiFunction.todoIdPathVariable() {
        pathVariable<Int>("todoId")
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
  }

  @Test
  fun `test webmvc dsl codegen`() {
    main()
  }
}
