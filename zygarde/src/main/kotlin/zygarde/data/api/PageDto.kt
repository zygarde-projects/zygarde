package puni.zygarde.data.dto

import io.swagger.annotations.ApiModel

@ApiModel
open class PageDto<T>(
  val atPage: Int,
  val totalPages: Int,
  val items: List<T>,
  val totalCount: Long
)
