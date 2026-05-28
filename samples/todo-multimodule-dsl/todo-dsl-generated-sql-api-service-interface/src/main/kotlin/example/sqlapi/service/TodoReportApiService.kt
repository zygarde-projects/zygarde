package example.sqlapi.service

import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import kotlin.collections.Collection

public interface TodoReportApiService {
  public fun searchTodos(req: SearchTodosReq): Collection<TodoReportDto>
}
