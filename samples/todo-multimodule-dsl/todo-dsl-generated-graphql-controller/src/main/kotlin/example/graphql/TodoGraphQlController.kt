package example.graphql

import example.graphql.service.TodoGraphQlService
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.Collection
import org.springframework.graphql.`data`.method.`annotation`.Argument
import org.springframework.graphql.`data`.method.`annotation`.MutationMapping
import org.springframework.graphql.`data`.method.`annotation`.QueryMapping
import org.springframework.stereotype.Controller
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq
import zygarde.core.di.DiServiceContext.bean

@Controller
public class TodoGraphQlController {
  @QueryMapping(name = "todos")
  public fun todos(@Argument(name = "filter") filter: TodoFilter?): Collection<TodoDto> {
    val service = bean<TodoGraphQlService>()
    return service.todos(filter)
  }

  @QueryMapping(name = "todo")
  public fun todo(@Argument(name = "id") id: Int): TodoDto? {
    val service = bean<TodoGraphQlService>()
    return service.todo(id)
  }

  @QueryMapping(name = "todosByIds")
  public fun todosByIds(@Argument(name = "ids") ids: Collection<Int>): Collection<TodoDto> {
    val service = bean<TodoGraphQlService>()
    return service.todosByIds(ids)
  }

  @MutationMapping(name = "createTodo")
  public fun createTodo(@Argument(name = "input") input: CreateTodoReq): TodoDto {
    val service = bean<TodoGraphQlService>()
    return service.createTodo(input)
  }

  @MutationMapping(name = "updateTodo")
  public fun updateTodo(@Argument(name = "id") id: Int, @Argument(name = "input")
      input: UpdateTodoReq): TodoDto {
    val service = bean<TodoGraphQlService>()
    return service.updateTodo(id, input)
  }

  @MutationMapping(name = "deleteTodo")
  public fun deleteTodo(@Argument(name = "id") id: Int): Boolean {
    val service = bean<TodoGraphQlService>()
    return service.deleteTodo(id)
  }
}
