package example.graphql.service

import example.graphql.TodoFilter
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.Collection
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq

public interface TodoGraphQlService {
  public fun todos(filter: TodoFilter?): Collection<TodoDto>

  public fun todo(id: Int): TodoDto?

  public fun createTodo(input: CreateTodoReq): TodoDto

  public fun updateTodo(id: Int, input: UpdateTodoReq): TodoDto

  public fun deleteTodo(id: Int): Boolean
}
