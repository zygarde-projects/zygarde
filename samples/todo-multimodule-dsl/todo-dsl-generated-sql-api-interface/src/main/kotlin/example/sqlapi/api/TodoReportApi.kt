package example.sqlapi.api

import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import kotlin.collections.Collection

public interface TodoReportApi {
  public fun searchTodos(req: SearchTodosReq): Collection<TodoReportDto>
}
