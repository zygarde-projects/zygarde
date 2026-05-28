package example.sqlapi.controller

import example.sqlapi.api.TodoCommandApi
import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.UpdateTodoBySqlReq
import example.sqlapi.service.TodoCommandApiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlin.Int
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.PutMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import zygarde.core.di.DiServiceContext.bean

@RestController
@Tag(name="TodoCommandApi")
public class TodoCommandApiController : TodoCommandApi {
  @PostMapping(value=["/api/todo-command/create"])
  @Operation(summary="createTodo")
  override fun createTodo(@RequestBody @Valid req: CreateTodoBySqlReq): CreatedTodoKeyDto {
    val service = bean<TodoCommandApiService>()
    val result = service.createTodo(req)
    return result
  }

  @PutMapping(value=["/api/todo-command/update/{id}"])
  @Operation(summary="updateTodo")
  override fun updateTodo(@PathVariable(value="id") id: Int, @RequestBody @Valid
      req: UpdateTodoBySqlReq): Int {
    val service = bean<TodoCommandApiService>()
    val result = service.updateTodo(id,req)
    return result
  }

  @DeleteMapping(value=["/api/todo-command/delete/{id}"])
  @Operation(summary="deleteTodo")
  override fun deleteTodo(@PathVariable(value="id") id: Int) {
    val service = bean<TodoCommandApiService>()
    service.deleteTodo(id)
  }
}
