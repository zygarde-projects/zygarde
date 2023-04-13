package zygarde.codegen.model.extensions

import example.Mark
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import zygarde.codegen.`data`.dto.MarkDetailDto
import zygarde.codegen.`data`.dto.TodoDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

public object MarkDetailDtoBuilder {
  public fun build(
    mark: Mark,
    extraStr: String,
    extraMap: Map<String, List<String>>,
    todo: TodoDto
  ): MarkDetailDto = MarkDetailDto(
  id = AutoIntIdValueProvider().getValue(mark),
  x = mark.x,
  y = mark.y,
  comments = mark.comments,
  extraStr = extraStr,
  extraMap = extraMap,
  todo = todo
  )
}
