package example.api

import kotlin.Int
import kotlin.Unit
import kotlin.collections.Collection
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.PutMapping
import org.springframework.web.bind.`annotation`.RequestBody
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`data`.dto.UpdateTodoReq

@FeignClient(name = "TodoApi")
public interface TodoApiFeign : TodoApi {
  @GetMapping(value = ["/api/todo"])
  public override fun getTodoList(): Collection<TodoDto>

  @GetMapping(value = ["/api/todo/{todoId}"])
  public override fun getTodo(@PathVariable(value = "todoId") todoId: Int): TodoDto

  @PostMapping(value = ["/api/todo"])
  public override fun createTodo(@RequestBody req: CreateTodoReq): TodoDto

  @PutMapping(value = ["/api/todo/{todoId}"])
  public override fun updateTodo(
    @PathVariable(value = "todoId") todoId: Int,
    @RequestBody
    req: UpdateTodoReq
  ): TodoDto

  @DeleteMapping(value = ["/api/todo/{todoId}"])
  public override fun deleteTodo(@PathVariable(value = "todoId") todoId: Int): Unit
}
