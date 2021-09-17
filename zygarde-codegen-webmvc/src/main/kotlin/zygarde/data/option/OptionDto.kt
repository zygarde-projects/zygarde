package zygarde.data.option

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class OptionDto(
  val key: String,
  val label: String,
  val active: Boolean = true
)
