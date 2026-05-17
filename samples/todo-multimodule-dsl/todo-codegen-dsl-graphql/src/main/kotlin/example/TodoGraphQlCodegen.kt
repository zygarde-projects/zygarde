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
        returns<TodoDto>("Todo", nullable = true)
        serviceName = "TodoGraphQlService"
      }

      query("todosByIds") {
        collectionArgument<Int>("ids")
        returnsCollection<TodoDto>("Todo")
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

      // `Todo` and `TodoInput` are derived from model-mapping DTOs instead of
      // being re-declared by hand. `TodoFilter` has no DTO and stays manual.
      typeFrom(TodoModelDslCodegen.TodoDtos.TodoDto, name = "Todo")
      inputFrom(TodoModelDslCodegen.TodoDtos.CreateTodoReq, name = "TodoInput")

      input("TodoFilter") {
        field<Int>("idEq", nullable = true)
        collectionField<Int>("idsIn", nullable = true)
        field<String>("descriptionContains", nullable = true)
      }
    }
  }
}
