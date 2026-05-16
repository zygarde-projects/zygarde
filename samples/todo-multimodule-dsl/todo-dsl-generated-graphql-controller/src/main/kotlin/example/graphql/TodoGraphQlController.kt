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
  @QueryMapping
  public fun todos(@Argument filter: TodoFilter?): Collection<TodoDto> {
    val service = bean<TodoGraphQlService>()
    return service.todos(filter)
  }

  @QueryMapping
  public fun todo(@Argument id: Int): TodoDto? {
    val service = bean<TodoGraphQlService>()
    return service.todo(id)
  }

  @QueryMapping
  public fun todosByIds(@Argument ids: Collection<Int>): Collection<TodoDto> {
    val service = bean<TodoGraphQlService>()
    return service.todosByIds(ids)
  }

  @MutationMapping
  public fun createTodo(@Argument input: CreateTodoReq): TodoDto {
    val service = bean<TodoGraphQlService>()
    return service.createTodo(input)
  }

  @MutationMapping
  public fun updateTodo(@Argument id: Int, @Argument input: UpdateTodoReq): TodoDto {
    val service = bean<TodoGraphQlService>()
    return service.updateTodo(id, input)
  }

  @MutationMapping
  public fun deleteTodo(@Argument id: Int): Boolean {
    val service = bean<TodoGraphQlService>()
    return service.deleteTodo(id)
  }
}
