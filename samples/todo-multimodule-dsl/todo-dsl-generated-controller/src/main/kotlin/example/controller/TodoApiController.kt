package example.controller

import example.api.TodoApi
import example.service.TodoApiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.lang.ThreadLocal
import javax.validation.Valid
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.PutMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq
import zygarde.core.di.DiServiceContext.bean

@RestController
@Tag(name = "TodoApi")
public class TodoApiController : TodoApi {
  public val createTodoThreadLocal: ThreadLocal<String> = ThreadLocal()

  @GetMapping(value = ["/api/todo"])
  @Operation(summary = "getTodoList")
  public override fun getTodoList(): Collection<TodoDto> {
    val service = bean<TodoApiService>()
    val result = service.getTodoList()
    return result
  }

  @GetMapping(value = ["/api/todo/{todoId}"])
  @Operation(summary = "getTodo")
  public override fun getTodo(@PathVariable(value = "todoId") todoId: Int): TodoDto {
    val service = bean<TodoApiService>()
    val result = service.getTodo(todoId)
    return result
  }

  @PostMapping(value = ["/api/todo"])
  @Operation(summary = "createTodo")
  public override fun createTodo(@RequestBody @Valid req: CreateTodoReq): TodoDto {
    val service = bean<TodoApiService>()
    val result = service.createTodo(req, { createTodoThreadLocal.set(it) })
    val extraParam = createTodoThreadLocal.get()
    service.createTodoPostProcessing(req, result, extraParam)
    return result
  }

  @PutMapping(value = ["/api/todo/{todoId}"])
  @Operation(summary = "updateTodo")
  public override fun updateTodo(
    @PathVariable(value = "todoId") todoId: Int,
    @RequestBody @Valid
    req: UpdateTodoReq
  ): TodoDto {
    val service = bean<TodoApiService>()
    val result = service.updateTodo(todoId, req)
    return result
  }

  @DeleteMapping(value = ["/api/todo/{todoId}"])
  @Operation(summary = "deleteTodo")
  public override fun deleteTodo(@PathVariable(value = "todoId") todoId: Int) {
    val service = bean<TodoApiService>()
    service.deleteTodo(todoId)
  }
}
