package example.graphql.service

import example.graphql.TodoFilter
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.Collection
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.UpdateTodoReq

public interface TodoGraphQlService {
  public fun todos(filter: TodoFilter?): Collection<TodoGraphQlSource>

  public fun todo(id: Int): TodoGraphQlSource?

  public fun todosByIds(ids: Collection<Int>): Collection<TodoGraphQlSource>

  public fun createTodo(input: CreateTodoReq): TodoGraphQlSource

  public fun updateTodo(id: Int, input: UpdateTodoReq): TodoGraphQlSource

  public fun deleteTodo(id: Int): Boolean
}
