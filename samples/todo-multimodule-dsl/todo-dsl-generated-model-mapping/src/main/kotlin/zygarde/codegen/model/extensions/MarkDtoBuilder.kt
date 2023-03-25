package zygarde.codegen.model.extensions

import example.Mark
import zygarde.codegen.`data`.dto.MarkDto
import zygarde.codegen.`value`.AutoIntIdValueProvider

public object MarkDtoBuilder {
  public fun build(mark: Mark): MarkDto = MarkDto(
  id = AutoIntIdValueProvider().getValue(mark),
  x = mark.x,
  y = mark.y
  )
}
