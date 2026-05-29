package example

import example.graphql.TodoFilter
import zygarde.codegen.data.dto.CreateTodoReq
import zygarde.codegen.data.dto.TodoDto
import zygarde.codegen.data.dto.UpdateTodoReq
import zygarde.codegen.dsl.graphql.GraphQlDslCodegen

class TodoGraphQlCodegen : GraphQlDslCodegen() {
  override fun codegen() {
    schema("TodoGraphQl") {
      type<TodoDto>("Todo") {
        fromAutoIntId(Todo::id)
        from(Todo::description)
        ref("file", "File", nullable = true)
      }

      type("File") {
        field<String>("id")
        field<String>("name")
      }

      type<GraphQlAuthor>("Author") {
        fromAutoIntId(GraphQlAuthor::id)
        from(GraphQlAuthor::name)
      }

      type<GraphQlBook>("Book") {
        fromAutoIntId(GraphQlBook::id)
        from(GraphQlBook::title)
        ref<GraphQlAuthor>("author", nullable = true)
      }

      input<CreateTodoReq>("TodoInput") {
        applyTo(Todo::description)
      }
      bindGraphQlType<UpdateTodoReq>("TodoInput")

      input("TodoFilter") {
        field<Int>("idEq", nullable = true)
        collectionField<Int>("idsIn", nullable = true)
        field<String>("descriptionContains", nullable = true)
      }
      bindGraphQlType<TodoFilter>("TodoFilter")

      query("todos") {
        argument<TodoFilter>("filter", nullable = true)
        returnsCollection<TodoDto>()
        serviceName = "TodoGraphQlService"
      }

      query("todo") {
        argument<Int>("id")
        returns<TodoDto>(nullable = true)
        serviceName = "TodoGraphQlService"
      }

      query("todosByIds") {
        collectionArgument<Int>("ids")
        returnsCollection<TodoDto>()
        serviceName = "TodoGraphQlService"
      }

      mutation("createTodo") {
        argument<CreateTodoReq>("input")
        returns<TodoDto>()
        serviceName = "TodoGraphQlService"
      }

      mutation("updateTodo") {
        argument<Int>("id")
        argument<UpdateTodoReq>("input")
        returns<TodoDto>()
        serviceName = "TodoGraphQlService"
      }

      mutation("deleteTodo") {
        argument<Int>("id")
        returns<Boolean>("Boolean")
        serviceName = "TodoGraphQlService"
      }
    }
  }
}
