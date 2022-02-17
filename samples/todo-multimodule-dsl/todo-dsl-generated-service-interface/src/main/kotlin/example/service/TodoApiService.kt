package example.service

import kotlin.Int
import kotlin.Unit
import kotlin.collections.Collection
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq

public interface TodoApiService {
  public fun getTodoList(): Collection<TodoDto>

  public fun getTodo(todoId: Int): TodoDto

  public fun createTodo(req: CreateTodoReq): TodoDto

  public fun updateTodo(todoId: Int, req: UpdateTodoReq): TodoDto

  public fun deleteTodo(todoId: Int): Unit
}
