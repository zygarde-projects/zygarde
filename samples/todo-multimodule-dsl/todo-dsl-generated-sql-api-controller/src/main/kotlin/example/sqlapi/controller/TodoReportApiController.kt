package example.sqlapi.controller

import example.sqlapi.api.TodoReportApi
import example.sqlapi.dto.SearchTodosReq
import example.sqlapi.dto.TodoReportDto
import example.sqlapi.service.TodoReportApiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlin.collections.Collection
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.RestController
import zygarde.core.di.DiServiceContext.bean

@RestController
@Tag(name="TodoReportApi")
public class TodoReportApiController : TodoReportApi {
  @GetMapping(value=["/api/todo-report/search"])
  @Operation(summary="searchTodos")
  override fun searchTodos(@ParameterObject @Valid req: SearchTodosReq): Collection<TodoReportDto> {
    val service = bean<TodoReportApiService>()
    val result = service.searchTodos(req)
    return result
  }
}
