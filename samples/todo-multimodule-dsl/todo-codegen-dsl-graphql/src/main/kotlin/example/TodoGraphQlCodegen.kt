package example

import example.graphql.TodoFilter
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.dsl.graphql.GraphQlDslCodegen

class TodoGraphQlCodegen : GraphQlDslCodegen() {
  override fun codegen() {
    schema("TodoGraphQl") {
      query("todos") {
        argument<TodoFilter>("filter", "TodoFilter", nullable = true)
        returnsCollection<TodoDto>("Todo")
        serviceName = "TodoGraphQlService"
      }

      query("todo") {
        argument<Int>("id")
        returns<TodoDto>("Todo")
        serviceName = "TodoGraphQlService"
      }

      mutation("createTodo") {
        argument<CreateTodoReq>("input", "TodoInput")
        returns<TodoDto>("Todo")
        serviceName = "TodoGraphQlService"
      }

      mutation("updateTodo") {
        argument<Int>("id")
        argument<UpdateTodoReq>("input", "TodoInput")
        returns<TodoDto>("Todo")
        serviceName = "TodoGraphQlService"
      }

      mutation("deleteTodo") {
        argument<Int>("id")
        returns<Boolean>("Boolean")
        serviceName = "TodoGraphQlService"
      }

      type("Todo") {
        field<Int>("id")
        field<String>("description")
      }

      input("TodoInput") {
        field<String>("description")
      }

      input("TodoFilter") {
        field<Int>("idEq", nullable = true)
        field<String>("descriptionContains", nullable = true)
      }
    }
  }
}
