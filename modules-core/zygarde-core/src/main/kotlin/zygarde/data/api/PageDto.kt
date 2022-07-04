package zygarde.data.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema
open class PageDto<T>(
  val atPage: Int,
  val totalPages: Int,
  val items: List<T>,
  val totalCount: Long
) {
  companion object {
    fun <T> empty(): PageDto<T> {
      return PageDto(0, 0, emptyList(), 0)
    }
  }
}
