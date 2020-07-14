package zygarde.data.api

import org.springframework.data.domain.Page
import puni.zygarde.data.dto.PageDto

fun <T, DTO> Page<T>.toPageDto(mapFunc: (entity: T) -> DTO): PageDto<DTO> {
  return PageDto(
    this.number + 1,
    this.totalPages,
    this.content.map(mapFunc),
    this.totalElements
  )
}
