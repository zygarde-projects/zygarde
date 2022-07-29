package zygarde.codegen.model.extensions

import example.Todo
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

public object TodoToDtoExtensions {
  public fun Todo.toTodoDto() = TodoDto(
    id = AutoIntIdValueProvider().getValue(this),
    description = this.description
  )
}
