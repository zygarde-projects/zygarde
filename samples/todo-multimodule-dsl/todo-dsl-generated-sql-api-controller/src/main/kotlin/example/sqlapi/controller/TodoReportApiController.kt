package example.sqlapi.controller

import example.sqlapi.api.TodoReportApi
import example.sqlapi.dto.TodoReportDto
import example.sqlapi.service.TodoReportApiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RequestParam
import org.springframework.web.bind.`annotation`.RestController
import zygarde.`data`.api.PageDto
import zygarde.core.di.DiServiceContext.bean

@RestController
@Tag(name="TodoReportApi")
public class TodoReportApiController : TodoReportApi {
  @GetMapping(value=["/api/todo-report/search"])
  @Operation(summary="searchTodos")
  override fun searchTodos(@RequestParam(value="keyword", required=false) keyword: String?):
      Collection<TodoReportDto> {
    val service = bean<TodoReportApiService>()
    val result = service.searchTodos(keyword)
    return result
  }

  @GetMapping(value=["/api/todo-report/find/{id}"])
  @Operation(summary="findTodo")
  override fun findTodo(@PathVariable(value="id") id: Int): TodoReportDto? {
    val service = bean<TodoReportApiService>()
    val result = service.findTodo(id)
    return result
  }

  @GetMapping(value=["/api/todo-report/page"])
  @Operation(summary="pageTodos")
  override fun pageTodos(
    @RequestParam(value="keyword", required=false) keyword: String?,
    @RequestParam(value="pageSize") pageSize: Int,
    @RequestParam(value="atPage") atPage: Int,
  ): PageDto<TodoReportDto> {
    val service = bean<TodoReportApiService>()
    val result = service.pageTodos(keyword,pageSize,atPage)
    return result
  }
}
