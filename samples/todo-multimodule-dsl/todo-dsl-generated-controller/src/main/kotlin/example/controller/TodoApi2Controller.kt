package example.controller

import example.api.TodoApi2
import example.service.TodoApiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlin.collections.Collection
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.RestController
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.core.di.DiServiceContext.bean

@RestController
@Tag(name="TodoApi2")
public class TodoApi2Controller : TodoApi2 {
  @GetMapping(value=["api/todo2"])
  @Operation(summary="getTodoList")
  public override fun getTodoList(): Collection<TodoDto> {
    val service = bean<TodoApiService>()
    val result = service.getTodoList()
    return result
  }
}
