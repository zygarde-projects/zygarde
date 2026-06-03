package example.api

import kotlin.Int
import kotlin.collections.Collection
import zygarde.`data`.api.PageDto
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.SearchTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq

public interface TodoApi {
  public fun getTodoList(): Collection<TodoDto>

  public fun searchTodos(req: SearchTodoReq): PageDto<TodoDto>

  public fun getTodo(todoId: Int): TodoDto

  public fun createTodo(req: CreateTodoReq): TodoDto

  public fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto

  public fun deleteTodo(todoId: Int)
}
