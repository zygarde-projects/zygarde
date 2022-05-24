package zygarde.codegen.model.extensions

import example.Note
import example.Todo
import kotlin.String
import zygarde.codegen.`data`.dto.TodoDetailDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

public object TodoDetailDtoBuilder {
  public fun build(
    todo: Todo,
    note: Note,
    remark: String
  ): TodoDetailDto = TodoDetailDto(
  id = AutoIntIdValueProvider().getValue(todo),
  description = todo.description,
  title = note.title,
  remark = remark
  )
}
