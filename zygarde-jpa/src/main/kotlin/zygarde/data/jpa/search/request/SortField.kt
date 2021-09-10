package zygarde.data.jpa.search.request

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

@Schema
data class SortField(
  @Schema(description = "排序方式")
  var sort: Sort.Direction? = null,
  @Schema(description = "排序欄位")
  var field: String? = null
) {
  fun toSort(): Sort? {
    return sort?.let { s -> field?.let { f -> Sort.by(s, f) } }
  }
}
