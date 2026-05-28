package example.sqlapi.api

import example.sqlapi.dto.FindTodoReq
import example.sqlapi.dto.PageTodosReq
import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import kotlin.collections.Collection
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.web.bind.`annotation`.GetMapping
import zygarde.`data`.api.PageDto

@FeignClient(name="TodoReportApi")
public interface TodoReportApiFeign : TodoReportApi {
  @GetMapping(value=["/api/todo-report/search"])
  override fun searchTodos(@SpringQueryMap req: SearchTodosReq): Collection<TodoReportDto>

  @GetMapping(value=["/api/todo-report/find"])
  override fun findTodo(@SpringQueryMap req: FindTodoReq): TodoReportDto?

  @GetMapping(value=["/api/todo-report/page"])
  override fun pageTodos(@SpringQueryMap req: PageTodosReq): PageDto<TodoReportDto>
}
