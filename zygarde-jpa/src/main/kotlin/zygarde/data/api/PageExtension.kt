package zygarde.data.api

import org.springframework.data.domain.Page

fun <T, DTO> Page<T>.toPageDto(mapFunc: (entity: T) -> DTO): PageDto<DTO> {
  return PageDto(
    this.number + 1,
    this.totalPages,
    this.content.map(mapFunc),
    this.totalElements
  )
}
