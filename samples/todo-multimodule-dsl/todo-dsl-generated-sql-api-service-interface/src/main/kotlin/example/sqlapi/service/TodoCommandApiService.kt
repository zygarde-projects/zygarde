package example.sqlapi.service

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.UpdateTodoBySqlReq
import kotlin.Int

public interface TodoCommandApiService {
  public fun createTodo(req: CreateTodoBySqlReq): CreatedTodoKeyDto

  public fun updateTodo(id: Int, req: UpdateTodoBySqlReq): Int

  public fun deleteTodo(id: Int)
}
