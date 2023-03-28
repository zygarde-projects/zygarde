package example.api

import kotlin.collections.Collection
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.`annotation`.GetMapping
import zygarde.codegen.`data`.dto.TodoDto

@FeignClient(name="TodoApi2")
public interface TodoApi2Feign : TodoApi2 {
  @GetMapping(value=["api/todo2"])
  public override fun getTodoList(): Collection<TodoDto>
}
