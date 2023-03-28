package example

import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.dsl.webmvc.DslApiFunction
import zygarde.codegen.dsl.webmvc.WebMvcDslCodegen

class TodoApiCodegen : WebMvcDslCodegen() {
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
        servicePostProcessing<String>()
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

    api("TodoApi2", "api/todo2") {
      get("getTodoList", "") {
        resCollection<TodoDto>()
        serviceName = "TodoApiService" // we can share TodoApiService that generated from TodoApi Spec
      }
    }
  }
}
