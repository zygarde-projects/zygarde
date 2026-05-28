package example.sqlapi.api

import example.sqlapi.dto.FindTodoReq
import example.sqlapi.dto.PageTodosReq
import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import kotlin.collections.Collection
import zygarde.`data`.api.PageDto

public interface TodoReportApi {
  public fun searchTodos(req: SearchTodosReq): Collection<TodoReportDto>

  public fun findTodo(req: FindTodoReq): TodoReportDto?

  public fun pageTodos(req: PageTodosReq): PageDto<TodoReportDto>
}
