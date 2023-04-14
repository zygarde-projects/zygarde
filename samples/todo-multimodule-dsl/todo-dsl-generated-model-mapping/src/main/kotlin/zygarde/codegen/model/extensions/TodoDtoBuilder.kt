package zygarde.codegen.model.extensions

import example.Todo
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

public object TodoDtoBuilder {
  public fun build(todo: Todo): TodoDto = TodoDto(
  id = AutoIntIdValueProvider().getValue(todo),
  description = todo.description
  )
}
