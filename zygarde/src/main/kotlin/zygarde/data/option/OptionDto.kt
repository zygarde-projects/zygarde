package zygarde.data.option

import io.swagger.annotations.ApiModel

@ApiModel
data class OptionDto(
  val key: String,
  val label: String,
  val active: Boolean = true
)
