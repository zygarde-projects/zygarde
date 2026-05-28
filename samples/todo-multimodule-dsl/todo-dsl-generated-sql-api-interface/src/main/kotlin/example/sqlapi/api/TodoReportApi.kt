package example.sqlapi.api

import example.sqlapi.dto.TodoReportDto
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import zygarde.`data`.api.PageDto

public interface TodoReportApi {
  public fun searchTodos(keyword: String?): Collection<TodoReportDto>

  public fun findTodo(id: Int): TodoReportDto?

  public fun pageTodos(
    keyword: String?,
    pageSize: Int,
    atPage: Int,
  ): PageDto<TodoReportDto>
}
