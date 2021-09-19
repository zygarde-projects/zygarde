package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

/**
 * @author leo
 */
@Schema
data class SortingRequest(
  @Schema(description = "排序方式")
  var sort: Sort.Direction = Sort.Direction.DESC,
  @Schema(description = "排序欄位")
  var sortFields: List<String> = emptyList()
)

fun SortingRequest?.asSort(): Sort {
  if (this != null && sortFields.isNotEmpty()) {
    return Sort.by(sort, *sortFields.toTypedArray())
  }
  return Sort.unsorted()
}
