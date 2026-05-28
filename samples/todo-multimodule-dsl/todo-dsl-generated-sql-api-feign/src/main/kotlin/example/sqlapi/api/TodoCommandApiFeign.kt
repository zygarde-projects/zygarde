package example.sqlapi.api

import example.sqlapi.dto.CreateTodoBySqlReq
import example.sqlapi.dto.CreatedTodoKeyDto
import example.sqlapi.dto.DeleteTodoBySqlReq
import example.sqlapi.dto.UpdateTodoBySqlReq
import kotlin.Int
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.PutMapping
import org.springframework.web.bind.`annotation`.RequestBody

@FeignClient(name="TodoCommandApi")
public interface TodoCommandApiFeign : TodoCommandApi {
  @PostMapping(value=["/api/todo-command/create"])
  override fun createTodo(@RequestBody req: CreateTodoBySqlReq): CreatedTodoKeyDto

  @PutMapping(value=["/api/todo-command/update"])
  override fun updateTodo(@RequestBody req: UpdateTodoBySqlReq): Int

  @DeleteMapping(value=["/api/todo-command/delete"])
  override fun deleteTodo(@SpringQueryMap req: DeleteTodoBySqlReq)
}
