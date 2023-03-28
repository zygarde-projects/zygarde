package example.api

import kotlin.collections.Collection
import zygarde.codegen.`data`.dto.TodoDto

public interface TodoApi2 {
  public fun getTodoList(): Collection<TodoDto>
}
