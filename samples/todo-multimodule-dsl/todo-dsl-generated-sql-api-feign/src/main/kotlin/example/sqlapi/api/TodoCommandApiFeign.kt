package example.sqlapi.api

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.UpdateTodoBySqlReq
import kotlin.Int
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.PutMapping
import org.springframework.web.bind.`annotation`.RequestBody

@FeignClient(name="TodoCommandApi")
public interface TodoCommandApiFeign : TodoCommandApi {
  @PostMapping(value=["/api/todo-command/create"])
  override fun createTodo(@RequestBody req: CreateTodoBySqlReq): CreatedTodoKeyDto

  @PutMapping(value=["/api/todo-command/update/{id}"])
  override fun updateTodo(@PathVariable(value="id") id: Int, @RequestBody req: UpdateTodoBySqlReq):
      Int

  @DeleteMapping(value=["/api/todo-command/delete/{id}"])
  override fun deleteTodo(@PathVariable(value="id") id: Int)
}
