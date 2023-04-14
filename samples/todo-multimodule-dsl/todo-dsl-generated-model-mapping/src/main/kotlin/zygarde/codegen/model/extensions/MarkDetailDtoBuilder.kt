package zygarde.codegen.model.extensions

import example.Mark
import kotlin.Any
import kotlin.Int
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
    extraMap1: Map<String, Any>,
    extraMap2: Map<String, Int?>,
    extraMap3: Map<String, List<String>>,
    extraMap4: Map<String, List<Int?>>,
    todo: TodoDto
  ): MarkDetailDto = MarkDetailDto(
  id = AutoIntIdValueProvider().getValue(mark),
  x = mark.x,
  y = mark.y,
  comments = mark.comments,
  extraStr = extraStr,
  extraMap1 = extraMap1,
  extraMap2 = extraMap2,
  extraMap3 = extraMap3,
  extraMap4 = extraMap4,
  todo = todo
  )
}
