package example.sqlapi.api

import example.sqlapi.dto.TodoReportDto
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RequestParam
import zygarde.`data`.api.PageDto

@FeignClient(name="TodoReportApi")
public interface TodoReportApiFeign : TodoReportApi {
  @GetMapping(value=["/api/todo-report/search"])
  override fun searchTodos(@RequestParam(value="keyword", required=false) keyword: String?):
      Collection<TodoReportDto>

  @GetMapping(value=["/api/todo-report/find/{id}"])
  override fun findTodo(@PathVariable(value="id") id: Int): TodoReportDto?

  @GetMapping(value=["/api/todo-report/find-by-ids"])
  override fun findTodosByIds(@RequestParam(value="ids") ids: Collection<Int>):
      Collection<TodoReportDto>

  @GetMapping(value=["/api/todo-report/page"])
  override fun pageTodos(
    @RequestParam(value="keyword", required=false) keyword: String?,
    @RequestParam(value="pageSize") pageSize: Int,
    @RequestParam(value="atPage") atPage: Int,
  ): PageDto<TodoReportDto>

  @GetMapping(value=["/api/todo-report/current"])
  override fun findCurrentTodo(): TodoReportDto?
}
