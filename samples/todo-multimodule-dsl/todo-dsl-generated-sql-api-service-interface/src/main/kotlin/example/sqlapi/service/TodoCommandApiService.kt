package example.sqlapi.service

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.DeleteTodoBySqlReq
import example.sqlapi.dto.UpdateTodoBySqlReq
import kotlin.Int

public interface TodoCommandApiService {
  public fun createTodo(req: CreateTodoBySqlReq): CreatedTodoKeyDto

  public fun updateTodo(req: UpdateTodoBySqlReq): Int

  public fun deleteTodo(req: DeleteTodoBySqlReq)
}
